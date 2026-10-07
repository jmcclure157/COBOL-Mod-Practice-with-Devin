# Migration Catalog: CardDemo COBOL → Java / Spring Boot

Every COBOL program in `carddemo/`, what it does, what it reads/writes, and where it should land in Java.
Update the **Status** column as programs are migrated.

Legend – Status: ✅ migrated · ⬜ not started · 🔸 stub only (entity/repository exists, no logic yet)

## Data files → JPA entities

VSAM files (keyed mainframe files) become tables. Entities live in `src/main/java/com/carddemo/model`.

| VSAM file (CICS name) | Copybook / record | Sample data (`carddemo/app/data/ASCII`) | Java entity | Seeded? |
|---|---|---|---|---|
| `ACCTDAT` | `CVACT01Y` `ACCOUNT-RECORD` (300 B) | `acctdata.txt` (50) | `Account` | ✅ |
| `CUSTDAT` | `CVCUS01Y` `CUSTOMER-RECORD` (500 B) | `custdata.txt` (50) | `Customer` | ✅ |
| `CARDDAT` | `CVACT02Y` `CARD-RECORD` (150 B) | `carddata.txt` (50) | `Card` | ✅ |
| `CARDXREF` / `CXACAIX` (alt. index by account) | `CVACT03Y` `CARD-XREF-RECORD` (50 B) | `cardxref.txt` (50) | `CardXref` | ✅ |
| `TRANSACT` | `CVTRA05Y` `TRAN-RECORD` (350 B) | — (written by `CBTRN02C`); seeded from `dailytran.txt` until that job is migrated | `Transaction` | ✅ |
| `DALYTRAN` (sequential input) | `CVTRA06Y` `DALYTRAN-RECORD` | `dailytran.txt` (300) | — (batch input, see `CBTRN02C`) | — |
| `TRANTYPE` | `CVTRA03Y` `TRAN-TYPE-RECORD` | `trantype.txt` (7) | `TransactionType` | ✅ |
| `TRANCATG` | `CVTRA04Y` `TRAN-CAT-RECORD` | `trancatg.txt` (18) | `TransactionCategory` | ✅ |
| `DISCGRP` | `CVTRA02Y` `DIS-GROUP-RECORD` | `discgrp.txt` (51) | `DisclosureGroup` | ✅ |
| `TCATBALF` | `CVTRA01Y` `TRAN-CAT-BAL-RECORD` | `tcatbal.txt` (50) | `TransactionCategoryBalance` | ✅ |
| `USRSEC` | `CSUSR01Y` `SEC-USER-DATA` | — (built by `DUSRSECJ.jcl`) | `UserSecurity` | 🔸 |

Other shared copybooks (not data records): `COCOM01Y` (COMMAREA – data passed between screens → HTTP session /
request params), `CSMSG01Y`/`CSMSG02Y` (messages), `CSDAT01Y` (date fields), `COTTL01Y` (screen titles),
`CVCRD01Y` (card-screen work fields), `CSUTLDPY`/`CSLKPCDY`/`CSSETATY` (validation helpers → Bean Validation),
`DFHAID`/`DFHBMSCA` (IBM CICS key/attribute constants, not needed in Java), `COxxxxx` (generated BMS map copybooks → DTOs).

## Core online programs (CICS)

Base path for source: `carddemo/app/cbl/`. Screens: `carddemo/app/bms/`.

| Program | Tran | BMS map | Purpose | VSAM files | Data copybooks | Proposed Java | Status |
|---|---|---|---|---|---|---|---|
| `COSGN00C` | CC00 | `COSGN00` | Sign-on, checks user id/password, routes admin vs. regular user | `USRSEC` | `CSUSR01Y` | Spring Security login (`POST /auth/login`), `UserSecurity` + `UserDetailsService` | ⬜ |
| `COMEN01C` | CM00 | `COMEN01` | Main menu for regular users | — | `COMEN02Y` (menu options) | Not needed as an API; becomes front-end navigation (optional `GET /menu`) | ⬜ |
| `COACTVWC` | CAVW | `COACTVW` | View account + customer details | `CARDXREF` (via `CXACAIX`), `ACCTDAT`, `CUSTDAT` | `CVACT01Y`, `CVACT03Y`, `CVCUS01Y` | `GET /accounts/{id}` → `AccountController` / `AccountViewService` | ✅ |
| `COACTUPC` | CAUP | `COACTUP` | Update account + customer (many field edits) | `CARDXREF`, `ACCTDAT`, `CUSTDAT` (READ UPDATE / REWRITE) | `CVACT01Y`, `CVACT03Y`, `CVCUS01Y` | `PUT /accounts/{id}` with Bean Validation, `@Transactional`, optimistic locking | ⬜ |
| `COCRDLIC` | CCLI | `COCRDLI` | List credit cards (paged, filter by account/card) | `CARDDAT` (browse) | `CVACT02Y` | `GET /cards?accountId=&cardNumber=&page=` | ⬜ |
| `COCRDSLC` | CCDL | `COCRDSL` | View one card | `CARDDAT` | `CVACT02Y`, `CVCUS01Y` | `GET /cards/{cardNumber}` | ⬜ |
| `COCRDUPC` | CCUP | `COCRDUP` | Update card (name, status, expiry) | `CARDDAT` (READ UPDATE / REWRITE) | `CVACT02Y` | `PUT /cards/{cardNumber}` | ⬜ |
| `COTRN00C` | CT00 | `COTRN00` | List transactions (paged browse forward/back) | `TRANSACT` (STARTBR/READNEXT/READPREV) | `CVTRA05Y` | `GET /transactions?startId=&page=` → `TransactionListService` + Spring Data `Slice` | ✅ |
| `COTRN01C` | CT01 | `COTRN01` | View one transaction | `TRANSACT` | `CVTRA05Y` | `GET /transactions/{id}` → `TransactionViewService` | ✅ |
| `COTRN02C` | CT02 | `COTRN02` | Add a transaction (validates card/account, next id) | `TRANSACT` (write), `CARDXREF`, `CXACAIX` | `CVTRA05Y`, `CVACT01Y`, `CVACT03Y` | `POST /transactions` | ⬜ |
| `CORPT00C` | CR00 | `CORPT00` | Request transaction report (submits batch job `TRANREPT` via internal reader) | — (writes JCL to an extrapartition TDQ, i.e. the internal reader) | `CVTRA05Y` | `POST /reports/transactions` that launches the Spring Batch job (async) | ⬜ |
| `COBIL00C` | CB00 | `COBIL00` | Pay full account balance: write payment transaction, zero the balance | `ACCTDAT` (update), `CXACAIX`, `TRANSACT` (write) | `CVACT01Y`, `CVACT03Y`, `CVTRA05Y` | `POST /accounts/{id}/payments` in one `@Transactional` service | ⬜ |
| `COADM01C` | CA00 | `COADM01` | Admin menu | — | `COADM02Y` | Front-end navigation; admin endpoints behind `ROLE_ADMIN` | ⬜ |
| `COUSR00C` | CU00 | `COUSR00` | List users | `USRSEC` (browse) | `CSUSR01Y` | `GET /admin/users?page=` | ⬜ |
| `COUSR01C` | CU01 | `COUSR01` | Add user | `USRSEC` (write) | `CSUSR01Y` | `POST /admin/users` (hash passwords!) | ⬜ |
| `COUSR02C` | CU02 | `COUSR02` | Update user | `USRSEC` (rewrite) | `CSUSR01Y` | `PUT /admin/users/{id}` | ⬜ |
| `COUSR03C` | CU03 | `COUSR03` | Delete user | `USRSEC` (delete) | `CSUSR01Y` | `DELETE /admin/users/{id}` | ⬜ |

## Core batch programs

JCL in `carddemo/app/jcl/`.

| Program | JCL job | Purpose | Files (DD names) | Data copybooks | Proposed Java | Status |
|---|---|---|---|---|---|---|
| `CBACT01C` | `READACCT` | Read and print all account records | `ACCTFILE` in; `OUTFILE`, `ARRYFILE`, `VBRCFILE` out | `CVACT01Y`, `CODATECN` | Utility: simple `CommandLineRunner`/test, or drop (debug tool) | ⬜ |
| `CBACT02C` | `READCARD` | Read and print all cards | `CARDFILE` | `CVACT02Y` | Drop or tiny export job (debug tool) | ⬜ |
| `CBACT03C` | `READXREF` | Read and print card cross-reference | `XREFFILE` | `CVACT03Y` | Drop or tiny export job (debug tool) | ⬜ |
| `CBCUS01C` | `READCUST` | Read and print all customers | `CUSTFILE` | `CVCUS01Y` | Drop or tiny export job (debug tool) | ⬜ |
| `CBACT04C` | `INTCALC` | Monthly interest: category balance × disclosure-group rate → interest transactions, update balances | `TCATBALF`, `XREFFILE`, `DISCGRP`, `ACCTFILE` in/out, `TRANSACT` out | `CVTRA01Y`, `CVTRA02Y`, `CVACT01Y`, `CVACT03Y`, `CVTRA05Y` | Spring Batch job `interestCalculationJob` (or `@Scheduled` monthly) | ⬜ |
| `CBTRN01C` | — (early version) | Validate daily transactions against xref/account (no posting) | `DALYTRAN`, `CUSTFILE`, `XREFFILE`, `CARDFILE`, `ACCTFILE`, `TRANFILE` | `CVTRA06Y`, `CVCUS01Y`, `CVACT03Y`, `CVACT02Y`, `CVACT01Y`, `CVTRA05Y` | Folded into the posting job's validation step | ⬜ |
| `CBTRN02C` | `POSTTRAN` | **Post daily transactions**: validate card/account/credit limit/expiry, write `TRANSACT`, update account + category balance, write rejects | `DALYTRAN` in; `TRANFILE`, `XREFFILE`, `ACCTFILE`, `TCATBALF` in/out; `DALYREJS` out | `CVTRA06Y`, `CVTRA05Y`, `CVACT01Y`, `CVACT03Y`, `CVTRA01Y` | Spring Batch `postTransactionsJob`: `FlatFileItemReader` (dailytran) → validating `ItemProcessor` → JPA writer; rejects via `SkipListener` | ⬜ |
| `CBTRN03C` | `TRANREPT` | Print transaction detail report for a date range | `TRANFILE`, `CARDXREF`, `TRANTYPE`, `TRANCATG`, `DATEPARM` in; `TRANREPT` out | `CVTRA05Y`, `CVACT03Y`, `CVTRA03Y`, `CVTRA04Y`, `CVTRA07Y` | Spring Batch report job (CSV/PDF) or `GET /reports/transactions?from=&to=` | ⬜ |
| `CBSTM03A` | `CREASTMT` | Produce account statements (text + HTML) | `STMTFILE`, `HTMLFILE` out; reads via `CBSTM03B` | `COSTM01`, `CUSTREC`, `CVACT01Y`, `CVACT03Y` | `StatementService` + template engine (Thymeleaf) in a batch job | ⬜ |
| `CBSTM03B` | (called by `CBSTM03A`) | File-access subroutine for statements | `TRNXFILE`, `XREFFILE`, `CUSTFILE`, `ACCTFILE` | — | Replaced by JPA repositories | ⬜ |
| `CBEXPORT` | `CBEXPORT` | Export customers/accounts/cards/xref/transactions into one multi-record file (branch migration) | 5 input files; `EXPFILE` out | `CVCUS01Y`, `CVACT01Y`, `CVACT02Y`, `CVACT03Y`, `CVTRA05Y`, `CVEXPORT` | Spring Batch export job (JSON/CSV) | ⬜ |
| `CBIMPORT` | `CBIMPORT` | Import the export file back into separate files, with error output | `EXPFILE` in; 5 outputs + `ERROUT` | same as `CBEXPORT` | Spring Batch import job | ⬜ |
| `COBSWAIT` | `WAITSTEP` | Sleep for N centiseconds between job steps | — | — | Not needed (scheduler handles timing) | ⬜ |
| `CSUTLDTC` | (called) | Date validation via the LE `CEEDAYS` service | — | — | `java.time.LocalDate.parse` + Bean Validation | ⬜ |

Utility JCL that only runs IBM tools (`IDCAMS`, `SORT`, `IEBGENER`, `SDSF`, `FTP`), e.g. `ACCTFILE`, `CUSTFILE`,
`TRANBKP`, `COMBTRAN`, `OPENFIL`/`CLOSEFIL`, `DEFGDG*`: these define/load/back up VSAM files. In Java they are
replaced by the JPA schema, `CardDemoDataLoader`, and normal database backups.

## Optional modules (stretch goals)

### `app/app-transaction-type-db2` (Db2)

| Program | Tran / JCL | Type | Purpose | Proposed Java | Status |
|---|---|---|---|---|---|
| `COTRTLIC` | CTLI / `COTRTLI` | CICS + Db2 | List / update / delete transaction types (cursor paging) | `GET/PUT/DELETE /admin/transaction-types` | ⬜ |
| `COTRTUPC` | CTTU / `COTRTUP` | CICS + Db2 | Add / edit a transaction type | `POST/PUT /admin/transaction-types` | ⬜ |
| `COBTUPDT` | `MNTTRDB2` | Batch + Db2 | Apply transaction-type changes from an input file | Spring Batch job or Flyway data migration | ⬜ |

### `app/app-vsam-mq` (IBM MQ)

| Program | Tran | Purpose | Proposed Java | Status |
|---|---|---|---|---|
| `CODATE01` | CDRD | Reply to an MQ request with the system date | `@JmsListener` (or plain `GET /system/date`) | ⬜ |
| `COACCT01` | CDRA | Reply to an MQ request with account details (`ACCTDAT`, `CVACT01Y`) | `@JmsListener` reusing `AccountViewService` | ⬜ |

### `app/app-authorization-ims-db2-mq` (IMS DB + Db2 + MQ)

| Program | Tran / JCL | Type | Purpose | Proposed Java | Status |
|---|---|---|---|---|---|
| `COPAUA0C` | CP00 | CICS, MQ-triggered | Approve/decline card authorization requests; store pending auths in IMS | `@JmsListener` → `AuthorizationService` | ⬜ |
| `COPAUS0C` | CPVS / `COPAU00` | CICS + IMS | Pending-authorization summary for an account | `GET /accounts/{id}/authorizations` | ⬜ |
| `COPAUS1C` | CPVD / `COPAU01` | CICS + IMS | Pending-authorization detail | `GET /authorizations/{id}` | ⬜ |
| `COPAUS2C` | (called) | CICS + Db2 | Mark an authorization as fraud | `POST /authorizations/{id}/fraud` | ⬜ |
| `CBPAUP0C` | `CBPAUP0J` | Batch + IMS | Delete expired pending authorizations | `@Scheduled` purge job | ⬜ |
| `PAUDBUNL` | `UNLDPADB` | Batch + IMS | Unload the pending-auth IMS database to flat files | Not needed (DB export) | ⬜ |
| `PAUDBLOD` | `LOADPADB` | Batch + IMS | Load the pending-auth IMS database from flat files | Not needed (seed loader) | ⬜ |
| `DBUNLDGS` | `UNLDGSAM` | Batch + IMS | Unload pending auths to GSAM files | Not needed | ⬜ |

## Recommended order

1. `COACTVWC` account view ✅
2. `COTRN00C` transaction list ✅ (plus `COTRN01C` transaction view ✅)
3. `COTRN02C` transaction add
4. `COBIL00C` bill payment
5. `CBTRN02C` batch posting job

Reasoning is in the [README](README.md#recommended-migration-order).
