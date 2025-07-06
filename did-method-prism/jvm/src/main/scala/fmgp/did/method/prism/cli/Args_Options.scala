package fmgp.did.method.prism.cli

import zio.cli.*
import fmgp.did.method.prism.DIDPrism

// Conventions https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html
// https://www.gnu.org/software/libc/manual/html_node/Argp.html

val blockfrostTokenOpt =
  Options.text("blockfrost-token").optional.??("Blockfrost's API token/key")

val didArg =
  Args
    .text("DID")
    .mapOrFail(str =>
      DIDPrism.fromString(str) match
        case Left(error)     => Left(HelpDoc.p(s"Fail to parse Arg: $error"))
        case Right(didPrisn) => Right(didPrisn)
    )

val indexerWorkDirAgr =
  Args
    .directory("work-directory", exists = Exists.Yes)
    .??("Indexer file system path to be used as a state storage")
    .mapOrFail(e => Right(e))
