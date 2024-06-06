# DID-Example

## Graph: users serviced by Domain and mediators
  - **Alice** - `alice.did.fmgp.app`
    - **Pat** (the prover)
  - **Bob** - `bob.did.fmgp.app`
    - **Dave**
    - **Ivan** (the issuer)
  - **Charlie** - `charlie.did.fmgp.app`
    - **Eve**
      - **Frank**
      - **Victor** (the verifier)
  - `fabio.did.fmgp.app`
  - `did.fmgp.app`

## test wscat

Send Message with wscat `wscat -c ws://localhost:8080 --host "alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json'`

Send Message with curl `curl -X POST localhost:8080 -H "host: alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -d '{}'`
  * '{}'

Check Messages with `curl 'http://localhost:8080/db' -H "host: alice.did.fmgp.app"`

**BasicMessage to Alice from Bob**
`wscat -c ws://localhost:8080 --host "alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -x '{"ciphertext":"2kZG7zupxREzVJPJPjqLljvR6Ylyw0VIMwv7i33HQx-cMbLp_zo9hueQQtw1TbZHa_UYfArB-sgSZW0WA-rWBQNKrztvAZJcdZ3_CoyTs2D5H-fYjDEGsdG_FzHgUvVQTM4ZVR39buoWUewBU-ri30dfxpWMj6Sb2sjIOtQ6OT-sA-8rs8CQG4cctFPo2JtyfkUsfQm5UWHzlrawKrLbFyqcOwgjzJfy0-T-hS93_p0S6Ask-iMsw3yP1dDU4EJGH09F5IGiX6UojFD9q60JHAXMbk4b1ggMTdbsqA9AZP-J_58T1gbolrd6yakunZSSeW1td0ADO3RFm3j_NbPn9kxhcx2U666K9PXBoYPU4E2OH2hNGTdIfQmwaEmZyql4KqtGD55UXEPl5nqB8exLm5TWCQVnCoGQHE0WLteWl6fIzWRlLLGAAlugaTuL9T0DCn0ZEOXBi-E_Wa7S9r2vtyBFilyNg1hEd9jVERzw-O09G1XBkcxP-wcX90klBP-CryrHzb-l_axLVu8_e9FGVYws_8jM5683n5FPItXaycgM3lurG7ZaRt8JZ_00rYjLWq2r0wkYXT2OSXCDp0Q8og14c7EvtTYJ3-hp4MJM3SnAj1OD5Qv25p4WSOIImYp76IyCqwWV4UbAZJzdU2H9-zdgZdT0WB_1OcJ8RvEXMenSGEnhEUvnIHJbNKtG_1Hk6ff_x11cSav6WqOEtBWsInqqxIHKy7sSiS-wwfhfkS0WTbYt7sXVPho8qUSsLqo6","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImUwZGxKQk9qSks4d1Jnem13bVM2TXdrS1RDSmRoMUZoMU1jRnJyUXFxRWcifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWliMkl1Wkdsa0xtWnRaM0F1WVhCd0x5SXNJbklpT2x0ZExDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMCM2TFNrR3kzZTJ6NTR1UDRVOUh5WEpYUnBhRjJ5dHNuVHVWZ2g2U05ObUN5R1pRWiIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJ0SGVUTmxNbm8xTkhWUU5GVTVTSGxZU2xoU2NHRkdNbmwwYzI1VWRWWm5hRFpUVGs1dFEzbEhXbEZhTGxaNk5rMXJhbVIzZG1ZNWFGZGpObWxpV201a1Z6bENPVGR6YVRreVJGTnJPV2hYUVdoSFdVSm5VRGxyVlVack9Gb3VVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRscFlqSkpkVnBIYkd0TWJWcDBXak5CZFZsWVFuZE1lVWx6U1c1SmFVOXNkR1JNUTBwb1NXcHdZa2x0VW5CYVIwNTJZbGN3ZG1ScVNXbFlXREFqTmt4VGEwZDVNMlV5ZWpVMGRWQTBWVGxJZVZoS1dGSndZVVl5ZVhSemJsUjFWbWRvTmxOT1RtMURlVWRhVVZvIiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ","recipients":[{"encrypted_key":"txOTbxUzITsOKF8blLw4LyK4ZVKIzfqi-cCRzPg9beLv2A4chxr_qx2HdLDYI_5FAMi5lyiKLYy5IL70fRIXZPmd2V0HpZuj","header":{"kid":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"}}],"tag":"72k9sVAjD0Kf5xQpwv5nq3qt5Uhzluyf98LUR2oh4DM","iv":"dWCTA0n15wUkT34ePgZCGw"}'`

**TrustPingWithRequestedResponse to Alice from Bob** - `MsgId=3b2f082a-41fc-46d0-a66c-ea68d5604cd1`
`wscat -c ws://localhost:8080 --host "alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -x `

`curl -X POST localhost:8080 -H "host: alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -d '{"ciphertext":"AKA9bmJKoEVP2xTC72Nt2SVp_fAOlgrXRWYKHzmi3z9VbODwzFY3ZgVbUt_9aa87Ckko_RDx2LRnwaIl4_y2Nadj5T0xnfqUIqJ-XOV_MfxOJeGQAEIrDhPFrrJvsNCsg16dJ9_pKOQerZcdwWoOygBQjqMgpFNcW-YUY27Dsaz7nAw00sSjanwmehTzqDpdQ2abHBtaNsi1NlR8Paxz24UDM1nfELpmWrLr6GdKRmL2XCbO7e_iFILyh5SZxaNqYnpcH9Axj4KuT57BiJrGattHerEXKSnj88XTKkIfYaSXZqp5Wr-PUJHzgB9aXIV36RY6cwcrLEMAdWjC-RGzBLcoeQ4QGuKl5-nsg68Raf5gQLBWEkeWQwWz6ysBdLm3E8nQ1QcEWo1xzYZUgTSpb94mVsRq30qrHbXefisfzWVBrIktqh3o7TY-B8H8CesVgkMTQS9ox67rvQritBbyYQh-0n6Jn7rqFJha0ojzaQbvAGNUb9fOS4nxekcKQhHPNKUMKxEqIbOt2ymouafqAxBE7mjxrxz9RkJqeJTq-RwjGBa_xcVQVzneIndb11qzOFUZ1P-Fd6n2HAMi9cMK7Vir2vIIwHhNwH6Dnp7eGcPxYwrszBInoY1nnZw6YoqMO7OZ6kl--DQ7Fb4AH6J71-aTtuJGAU8K0JC5bypIDPj838yjYkDIwKT9WT624RtYg1pp9T9az1CzuDVlQ1JYnE6LJBDoxdftB7zixdAgE8k","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IjF3TUMwdFEzanRxMHEtdl8tbkFBd1J3bGVyU3c1eUNqVndhS2FHX3psZ28ifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWliMkl1Wkdsa0xtWnRaM0F1WVhCd0x5SXNJbklpT2x0ZExDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMCM2TFNrR3kzZTJ6NTR1UDRVOUh5WEpYUnBhRjJ5dHNuVHVWZ2g2U05ObUN5R1pRWiIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJ0SGVUTmxNbm8xTkhWUU5GVTVTSGxZU2xoU2NHRkdNbmwwYzI1VWRWWm5hRFpUVGs1dFEzbEhXbEZhTGxaNk5rMXJhbVIzZG1ZNWFGZGpObWxpV201a1Z6bENPVGR6YVRreVJGTnJPV2hYUVdoSFdVSm5VRGxyVlVack9Gb3VVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRscFlqSkpkVnBIYkd0TWJWcDBXak5CZFZsWVFuZE1lVWx6U1c1SmFVOXNkR1JNUTBwb1NXcHdZa2x0VW5CYVIwNTJZbGN3ZG1ScVNXbFlXREFqTmt4VGEwZDVNMlV5ZWpVMGRWQTBWVGxJZVZoS1dGSndZVVl5ZVhSemJsUjFWbWRvTmxOT1RtMURlVWRhVVZvIiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ","recipients":[{"encrypted_key":"RPtaGUPv22qIBA14BpkrD620HgOFAzXeZOIiVzeUag00-RHZs3XNC68XCqCgON5x4uQho8HkJ5MWFaTZNbkIizfm-zymOZ62","header":{"kid":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"}}],"tag":"nExhi83YT0n7B9q-OtfktTkdHO_UWk4a-KP0j_Oddwk","iv":"kXtzZmq3y8NAEBEC4NtDrg"}'`