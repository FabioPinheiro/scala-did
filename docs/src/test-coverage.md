# Test coverage

1. `sbt clean coverage testJVM` - Run test
2. `sbt coverageReport` - Generate reports
3. `sbt coverageAggregate` - Aggregate reports

## Report 

|    date    |   all   |   did   | did-imp | did-framework | did-resolver-web | did-resolver-peer | multiformats |
|:----------:|:-------:|:-------:|:-------:|:-------------:|:----------------:|:-----------------:|:------------:|
| 2022-11-26 | 29.21 % | 25.31 % | 57.60 % |      NA       |     81.58 %      |      27.50 %      |      NA      |
| 2022-01-31 | 18.88 % | 14.25 % | 39.63 % |    19.53 %    |     81.58 %      |      34.58 %      |   85.77 %    |


## Report notes

The aggregated report contends reports from the unpublished modules.

## Report output

You should open the reports with your browser. The reports will be in each module `target/scala-<scala-version>/scoverage-report`
- `all/aggregate` -> `/target/scala-3.3.1/scoverage-report/index.html`
- `did` -> `/did/jvm/target/scala-3.3.1/scoverage-report/index.html`
- `did-imp` -> `/did-imp/jvm/target/scala-3.3.1/scoverage-report/index.html`
- `did-framework` -> `/did-framework/jvm/target/scala-3.3.1/scoverage-report/index.html`
- `did-resolver-web` -> `/did-resolver-web/jvm/target/scala-3.3.1/scoverage-report/index.html`
- `did-resolver-peer` -> `/did-resolver-peer/jvm/target/scala-3.3.1/scoverage-report/index.html`