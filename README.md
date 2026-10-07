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
- Seeded today: accounts, customers, cards, card cross-references, transaction types/categories,
  disclosure groups, category balances. **Not seeded: `TRANSACT`** (posted transactions). On the mainframe that
  file is produced by the batch posting job (`CBTRN02C`) from `dailytran.txt`; it gets filled once that job is
  migrated. Until then the `Transaction` entity is a stub.

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

H2 console (browse the seeded tables): http://localhost:8080/h2-console, JDBC URL `jdbc:h2:mem:carddemo`, user `sa`, no password.

## Recommended migration order

Each step reuses what the previous one built, and gets slightly harder:

1. **Account view – `COACTVWC`** ✅ done. Read-only, three file reads, no updates. Introduces entities, seeding, REST.
2. **Transaction list – `COTRN00C`** → `GET /transactions?cardNumber=&page=`. Read-only, but adds paging
   (COBOL `STARTBR`/`READNEXT`/`READPREV` browse → Spring Data `Pageable`). Needs some `TRANSACT` data seeded first.
3. **Transaction add – `COTRN02C`** → `POST /transactions`. First write: input validation, cross-reference lookup,
   generating the next transaction id.
4. **Bill payment – `COBIL00C`** → `POST /accounts/{id}/payments`. Updates two records (writes a transaction and
   reduces the account balance) that must succeed or fail together → `@Transactional`.
5. **Batch posting job – `CBTRN02C` (`POSTTRAN.jcl`)** → Spring Batch job. Reads `dailytran.txt`, validates each
   record, posts to `TRANSACT`, updates account and category balances, writes rejects. First batch migration.

After that, good candidates are the other read-only screens (`COCRDLIC`, `COCRDSLC`, `COUSR00C`), then the
update screens (`COACTUPC`, `COCRDUPC`), sign-on/security (`COSGN00C` → Spring Security), and the remaining batch jobs.
See `MIGRATION.md` for the full list.

## Practice workflow

For each program: read the COBOL in `carddemo/`, note its paragraphs and file reads in `MIGRATION.md`,
write the Java with tests that pin the COBOL behaviour (same validations, same messages), then mark it migrated.
