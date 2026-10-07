# COBOL → Java Modernization Practice (CardDemo)

A practice repo for learning how to migrate a mainframe COBOL application to Java / Spring Boot,
one screen (or batch job) at a time.

- `carddemo/` – the **original, untouched** COBOL source of AWS's open-source
  [CardDemo](https://github.com/aws-samples/aws-mainframe-modernization-carddemo) sample
  (imported from commit `59cc6c2`). Treat it as the "legacy system" you are reading from.
- Everything else (`pom.xml`, `src/`) – the **new** Spring Boot application you migrate into.
- [`MIGRATION.md`](MIGRATION.md) – catalog of every COBOL program, what it does, which files/copybooks
  it uses, and its proposed Java replacement. Tracks migration status.

## How the COBOL app is structured

CardDemo is a credit card management system. Customers have accounts, accounts have cards, and cards
have transactions. Its pieces:

| Folder | What it is | Java equivalent |
|---|---|---|
| `carddemo/app/cbl/CO*.cbl` | **CICS online programs** (CICS = the mainframe's transaction server; each program drives one green-screen page) | `@RestController` + `@Service` |
| `carddemo/app/bms/*.bms` | **BMS mapsets** (screen layouts for 3270 terminals: which fields go where) | request/response JSON records (DTOs) |
| `carddemo/app/cbl/CB*.cbl` | **Batch programs** (run overnight, process whole files) | Spring Batch jobs or `@Scheduled` services |
| `carddemo/app/jcl/*.jcl` | **JCL** (job scripts that say which batch program to run with which files) | job configuration / scheduler |
| `carddemo/app/cpy/*.cpy` | **Copybooks** (shared record layouts, like a class definition for a fixed-width file row) | JPA `@Entity` classes in `com.carddemo.model` |
| `carddemo/app/data/ASCII/*.txt` | Sample data, one fixed-width record per line | seeded into H2 at startup |
| `carddemo/app/app-*/` | Optional add-ons using Db2, IMS and MQ | later / stretch goals |

The data lives in **VSAM KSDS files** (keyed files, roughly a single-table database each):
`ACCTDAT` (accounts), `CUSTDAT` (customers), `CARDDAT` (cards), `CARDXREF` (card ↔ customer ↔ account links),
`TRANSACT` (posted transactions), `USRSEC` (users) and a few reference files. In Java each becomes a table.

## The Java target

```
src/main/java/com/carddemo
├── CardDemoApplication.java   Spring Boot entry point
├── controller/                REST endpoints (replace CICS screens)
├── service/                   business logic (the PROCEDURE DIVISION, minus screen handling)
├── repository/                Spring Data JPA repositories (replace EXEC CICS READ / file I/O)
├── model/                     JPA entities, one per copybook record
└── seed/                      loads the original fixed-width sample files into H2
```

Key design notes:

- **Entities mirror copybooks.** Every field has a comment with its COBOL name and `PIC` clause
  (e.g. `ACCT-CURR-BAL PIC S9(10)V99` → `BigDecimal currentBalance`).
- **Data is loaded from the real sample files.** `CopybookReader` reads fixed-width records and decodes
  COBOL "zoned decimal" signed numbers (the sign is hidden in the last character: `{`/`A`–`I` = positive,
  `}`/`J`–`R` = negative). The files are packaged as `classpath:carddemo-data/` straight from
  `carddemo/app/data/ASCII`, so there is a single copy of the data.
- **H2 in-memory database**, recreated on each start. Switch `spring.datasource.*` to PostgreSQL later if you want.
- Seeded from the files: accounts, customers, cards, card cross-references, transaction types/categories,
  disclosure groups and category balances.
- **Transactions are posted, not seeded.** Like on the mainframe, `TRANSACT` starts empty. Right after seeding, the
  app runs the migrated posting job (`CBTRN02C`) once on `dailytran.txt`: 262 records are posted (and update the
  account and category balances) and 38 are rejected as `OVERLIMIT TRANSACTION`. So account 1 shows 1288.10, not
  the 194.00 in `acctdata.txt`.

## Build and run

Requires Java 21 and Maven.

```bash
mvn test                 # unit + integration tests
mvn spring-boot:run      # starts on http://localhost:8080
```

### First migrated flow: account view (`COACTVWC` → `GET /accounts/{id}`)

```bash
curl http://localhost:8080/accounts/1
curl http://localhost:8080/accounts/00000000001     # zero-padded 11-digit id also works
curl http://localhost:8080/accounts/abc             # 400: "Account Filter must  be a non-zero 11 digit number"
curl http://localhost:8080/accounts/99999999999     # 404: "Account:99999999999 not found in Cross ref file."
```

Same lookup order as the COBOL program: cross-reference (to find the customer) → account master → customer master.
Error messages are the original COBOL screen messages, returned as `ProblemDetail` JSON.

### Second migrated flow: transaction list (`COTRN00C` → `GET /transactions`)

```bash
curl http://localhost:8080/transactions                       # page 1: first 10 transactions in id order
curl "http://localhost:8080/transactions?page=2"              # PF8 (next page); page=1 is back to the top
curl "http://localhost:8080/transactions?startId=25430891"    # the screen's "Search Tran ID" field
curl "http://localhost:8080/transactions?startId=abc"         # 400: "Tran ID must be Numeric ..."
```

Like the COBOL screen: 10 rows per page, sorted by transaction id, showing id, date, description and amount,
and listing every transaction (the screen has no card or account filter). `hasNextPage` is the COBOL
"read one more record to see if PF8 has anything" check.

### Third migrated flow: view one transaction (`COTRN01C` → `GET /transactions/{id}`)

```bash
curl http://localhost:8080/transactions/0000000000683580   # every field of the COTRN01 screen
curl http://localhost:8080/transactions/683580             # 404: "Transaction ID NOT found..." (key must be the full 16 characters)
curl "http://localhost:8080/transactions/%20"              # 400: "Tran ID can NOT be empty..."
```

Like the COBOL screen, the ID is looked up exactly as typed (no zero-padding), and the two timestamps are shown as
dates. The COBOL `READ ... UPDATE` lock is dropped: the screen never updates the record.

### Fourth migrated flow: add a transaction (`COTRN02C` → `POST /transactions`)

```bash
curl -i -X POST http://localhost:8080/transactions -H 'Content-Type: application/json' -d '{
  "accountId": "50", "typeCode": "01", "categoryCode": "0001", "source": "POS TERM",
  "description": "Purchase at Test Store", "amount": "+00000123.45",
  "originDate": "2026-10-07", "processedDate": "2026-10-07", "merchantId": "800000000",
  "merchantName": "Test Store", "merchantCity": "Dallas", "merchantZip": "75201", "confirm": "Y"}'
# 201 Created, Location: /transactions/0000000996722788
# {"transactionId":"0000000996722788","message":"Transaction added successfully.  Your Tran ID is 0000000996722788."}
```

Like the COBOL screen:
- Give an `accountId` *or* a `cardNumber`. An account is looked up in the cross-reference to find its card, and an account wins if both are sent.
- Every field is required, and the edits run in the screen's order, stopping at the first failure. Each failure returns the COBOL message, for example `"Amount should be in format -99999999.99"` or `"Orig Date - Not a valid date..."`.
- `confirm` must be `Y`. `N` or blank returns `"Confirm to add this transaction..."`, because the screen asked before saving.
- The new id is the highest existing id + 1 (`READPREV` from `HIGH-VALUES`).
- Unknown account or card returns 404, a duplicate id returns 409, and every other failed edit returns 400.

The app's database lives in memory, so added transactions disappear when it restarts.

### Fifth migrated flow: bill payment (`COBIL00C` → `POST /accounts/{id}/payments`)

```bash
curl -i -X POST http://localhost:8080/accounts/2/payments -H 'Content-Type: application/json' -d '{"confirm": "Y"}'
# 201 Created, Location: /transactions/0000000996722788
# {"transactionId":"0000000996722788","amountPaid":1734.97,"newBalance":0.00,
#  "message":"Payment successful.  Your Transaction ID is 0000000996722788."}
```

Like the COBOL screen:
- It always pays the **whole** current balance. It writes a payment transaction (type `02`, category `2`,
  `BILL PAYMENT - ONLINE`, merchant `999999999`, the account's card) and sets the balance to zero.
- Both changes run in one `@Transactional` method, so either both are saved or neither is. The account is read
  with a database lock, like the COBOL `READ ... UPDATE`, so two payments can't pay the same balance twice.
- `confirm` must be `Y`. `N`, blank or no body returns `"Confirm to make a bill payment..."`; anything else returns
  `"Invalid value. Valid values are (Y/N)..."`.
- A balance of zero or less returns `"You have nothing to pay..."`, and an unknown account `"Account ID NOT found..."` (404).

### Sixth migrated flow: nightly posting job (`CBTRN02C` / `POSTTRAN.jcl` → Spring Batch `postTransactionsJob`)

There is no URL: it runs once at startup, after the data files are loaded and before the web server starts taking
requests (`PostTransactionsAtStartup`, like the mainframe's batch window when the online side is down). It is skipped
when `TRANSACT` already has rows. The log shows the COBOL's closing counts:

```text
TRANSACTIONS PROCESSED :300
TRANSACTIONS REJECTED  :38
```

Like the COBOL program, for each `dailytran.txt` record in file order:
- Checks, in order: card in the cross-reference (`100 INVALID CARD NUMBER FOUND`), account exists
  (`101 ACCOUNT RECORD NOT FOUND`), cycle credit − cycle debit + amount within the credit limit
  (`102 OVERLIMIT TRANSACTION`), received on or before the account's expiry date
  (`103 TRANSACTION RECEIVED AFTER ACCT EXPIRATION`). Both 102 and 103 are checked; if both fail, 103 is kept.
- A good record: adds the amount to its category balance (creating that row if missing), adds it to the account
  balance and to the cycle credit (amount ≥ 0) or cycle debit (amount < 0), and is written to `TRANSACT` with its own
  id and a processed timestamp in the COBOL's DB2 format (`2026-10-07-14.28.36.460000`).
- A bad record goes to the rejects file, `target/dalyrejs.txt` (`carddemo.posting.rejects-file`): the original
  350-byte record + 4-digit reason + 76-character description, 430 bytes like the `DALYREJS` dataset.
- Any rejects end the job as `COMPLETED WITH REJECTS` (the COBOL's return code 4). A file error, such as a
  duplicate transaction id, fails the job and stops the app from starting (the COBOL abends).

Spring Batch pieces: `FlatFileItemReader` (DALYTRAN) → parse → `PostingItemWriter`, which calls
`TransactionPostingService` for each record and writes rejects with a `FlatFileItemWriter` (DALYREJS). The job
parameters `dailyFile` and `rejectsFile` take the place of the JCL `DD` statements.

H2 console (browse the seeded tables): http://localhost:8080/h2-console, JDBC URL `jdbc:h2:mem:carddemo`, user `sa`, no password.

## Recommended migration order

Each step reuses what the previous one built, and gets slightly harder:

1. **Account view – `COACTVWC`** ✅ done. Read-only, three file reads, no updates. Introduces entities, seeding, REST.
2. **Transaction list – `COTRN00C`** ✅ done → `GET /transactions?startId=&page=` (plus `COTRN01C` view ✅ → `GET /transactions/{id}`). Read-only, but adds paging
   (COBOL `STARTBR`/`READNEXT`/`READPREV` browse → Spring Data `Slice`).
3. **Transaction add – `COTRN02C`** ✅ done → `POST /transactions`. First write: input validation, cross-reference lookup,
   generating the next transaction id.
4. **Bill payment – `COBIL00C`** ✅ done → `POST /accounts/{id}/payments`. Updates two records (writes a transaction and
   reduces the account balance) that must succeed or fail together → `@Transactional`.
5. **Batch posting job – `CBTRN02C` (`POSTTRAN.jcl`)** ✅ done → Spring Batch `postTransactionsJob`, run at startup. Reads `dailytran.txt`, validates each
   record, posts to `TRANSACT`, updates account and category balances, writes rejects. First batch migration.

After that, good candidates are the other read-only screens (`COCRDLIC`, `COCRDSLC`, `COUSR00C`), then the
update screens (`COACTUPC`, `COCRDUPC`), sign-on/security (`COSGN00C` → Spring Security), and the remaining batch jobs.
See `MIGRATION.md` for the full list.

## Practice workflow

For each program: read the COBOL in `carddemo/`, note its paragraphs and file reads in `MIGRATION.md`,
write the Java with tests that pin the COBOL behaviour (same validations, same messages), then mark it migrated.
