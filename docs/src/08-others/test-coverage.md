# Test coverage

1. `sbt clean coverage testJVM` - Run tests
2. `sbt coverageReport` - Generate reports
3. `sbt coverageAggregate` - Aggregate reports

## Report 

### Statement coverage
|    date    |   all   |   did   | did-imp | did-framework | did-resolver-web | did-resolver-peer | uniresolver | multiformats |
|:----------:|:-------:|:-------:|:-------:|:-------------:|:----------------:|:-----------------:|:-----------:|:------------:|
| 2022-11-26 | 29.21 % | 25.31 % | 57.60 % |      NA       |     81.58 %      |      27.50 %      |     NA      |      NA      |
| 2022-01-31 | 18.88 % | 14.25 % | 39.63 % |      NA       |     81.58 %      |      34.58 %      |     NA      |   85.77 %    |
| 2023-12-01 | 12.79 % | 10.82 % | 44.02 % |      NA       |     81.58 %      |      81.58 %      |   12.33 %   |   85.77 %    |
| 2024-03-03 | 12.30 % | 10.39 % | 46.11 % |      NA       |     81.58 %      |      30.73 %      |   12.50 %   |   85.77 %    |
| 2024-03-04 | 25.87 % | 23.67 % | 85.06 % |      NA       |     81.58 %      |      44.61 %      |   32.41 %   |   93.11 %    |


### Branch coverage

|    date    |   all   |   did   | did-imp | did-framework | did-resolver-web | did-resolver-peer | uniresolver | multiformats |
|:----------:|:-------:|:-------:|:-------:|:-------------:|:----------------:|:-----------------:|:-----------:|:------------:|
| 2023-12-01 | 36.65 % | 29.70 % | 30.77 % |      NA       |     75.00 %      |       0 %?        |  100.00 %   |   81.82 %    |
| 2024-03-03 | 37.84 % | 29.06 % | 39.29 % |      NA       |     75.00 %      |      50.00 %      |  100.00 %   |   81.82 %    |
| 2024-03-04 | 17.65 % | 11.03 % | 63.75 % |      NA       |     75.00 %      |      45.56 %      |   0.00 %    |   88.46 %    |
| 0000-00-00 |    %    |    %    |    %    |      NA       |        %         |         %         |      %      |      %       |

## Report notes

The aggregated report contends reports from the unpublished modules.
From 2024-03-03 coverage to 2024-03-04 coverage the sbt-scalafix ad update to 0.12.0, also the Scala version as update from 3.3.1 to 3.3.3.

## Report output

You should open the reports with your browser. The reports will be in each module `target/scala-<scala-version>/scoverage-report`
- `all/aggregate` -> `/target/scala-3.3.3/scoverage-report/index.html`
- `did` -> `/did/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `did-imp` -> `/did-imp/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `did-framework` -> `/did-framework/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `did-resolver-web` -> `/did-method-web/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `did-resolver-peer` -> `/did-method-peer/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `uniresolver` -> `/did-uniresolver/jvm/target/scala-3.3.3/scoverage-report/index.html`
- `multiformats` -> `/multiformats/jvm/target/scala-3.3.3/scoverage-report/index.html`
