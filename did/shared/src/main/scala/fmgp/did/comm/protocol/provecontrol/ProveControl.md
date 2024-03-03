# Prove Control Protocol


-> Request verification
<- Verification Challenge
-> Prove
<- Confirm Verification

type verificationType:
- Email
- DID
- Discord
- Tel
- Domain
- IP
- Address

fmgp.app/1/requestverification
 - `to`
 - `from`
 - `verificationType`
 - `subject`
fmgp.app/1/verificationchallenge
 - `to`
 - `from`
 - `verificationType`
 - `subject`
 - `secret` (only the 'to' can see)
fmgp.app/1/prove
 - `to`
 - `from`
 - `verificationType`
 - `subject`
 - `proof`
fmgp.app/1/confirmverification
 - `to`
 - `from`
 - `verificationType`
 - `subject`
 - `attachments`


## Calculate Proof

```scala
def calculateProof(
    verifier: DIDSubject, // TO in Prove == FROM in VerificationChallenge
    hoder: DIDSubject, // FROM in Prove == TO in VerificationChallenge
    verificationType: VerificationType,
    subject: String,
    secret: String,
) = SHA256.digestToHex(s"$verifier|$hoder|$verificationType|$subject|$secret")
```

# Connection Gateway Protocol

-> Register
<- Registration