package fmgp.multiformats

enum CodecStatus { case PERMANENT, DRAFT, DEPRECATED }
enum CodecTag {
  case SERIALIZATION, MULTIFORMAT, MULTIHASH, MULTIADDR, IPLD, NAMESPACE, CID, KEY, LIBP2P, TRANSPORT, MULTISIG, VARSIG,
    VALD, NONCE, MULTIKEY, ENCRYPTION, FILECOIN, HOLOCHAIN, SOFTHASH, ZEROCERT,
    HASH /* HASH is used to tag Non-cryptographic hash */
}

import CodecStatus.*
import CodecTag.*

object Codec {
  type AllHash = Codec
}

/** https://github.com/multiformats/multicodec/blob/352d05ad430713088e867216152725f581387bc8/table.csv
  *
  * Code Generation: sbt "multiformatsJVM/Test/runMain fmgp.multiformats.CodeGenerationHelper" >>
  * multiformats/shared/src/main/scala/fmgp/multiformats/Multicodec.aux
  */
enum Codec(val name: String, val tag: CodecTag, val code: Int, val status: CodecStatus) {

  def varint = Varint.encodeInt(code)
  def multicodec(data: Array[Byte]): Multicodec = Multicodec(this, data)

  /** raw binary */
  case identity extends Codec("identity", MULTIHASH, 0x00, PERMANENT)

  /** CIDv1 */
  case cidv1 extends Codec("cidv1", CID, 0x01, PERMANENT)

  /** CIDv2 */
  case cidv2 extends Codec("cidv2", CID, 0x02, DRAFT)

  /** CIDv3 */
  case cidv3 extends Codec("cidv3", CID, 0x03, DRAFT)
  case ip4 extends Codec("ip4", MULTIADDR, 0x04, PERMANENT)
  case tcp extends Codec("tcp", MULTIADDR, 0x06, PERMANENT)
  case sha1 extends Codec("sha1", MULTIHASH, 0x11, PERMANENT)
  case sha2_256 extends Codec("sha2-256", MULTIHASH, 0x12, PERMANENT)
  case sha2_512 extends Codec("sha2-512", MULTIHASH, 0x13, PERMANENT)
  case sha3_512 extends Codec("sha3-512", MULTIHASH, 0x14, PERMANENT)
  case sha3_384 extends Codec("sha3-384", MULTIHASH, 0x15, PERMANENT)
  case sha3_256 extends Codec("sha3-256", MULTIHASH, 0x16, PERMANENT)
  case sha3_224 extends Codec("sha3-224", MULTIHASH, 0x17, PERMANENT)
  case shake_128 extends Codec("shake-128", MULTIHASH, 0x18, DRAFT)
  case shake_256 extends Codec("shake-256", MULTIHASH, 0x19, DRAFT)

  /** keccak has variable output length. The number specifies the core length */
  case keccak_224 extends Codec("keccak-224", MULTIHASH, 0x1a, DRAFT)
  case keccak_256 extends Codec("keccak-256", MULTIHASH, 0x1b, DRAFT)
  case keccak_384 extends Codec("keccak-384", MULTIHASH, 0x1c, DRAFT)
  case keccak_512 extends Codec("keccak-512", MULTIHASH, 0x1d, DRAFT)

  /** BLAKE3 has a default 32 byte output length. The maximum length is (2^64)-1 bytes. */
  case blake3 extends Codec("blake3", MULTIHASH, 0x1e, DRAFT)

  /** aka SHA-384; as specified by FIPS 180-4. */
  case sha2_384 extends Codec("sha2-384", MULTIHASH, 0x20, PERMANENT)
  case dccp extends Codec("dccp", MULTIADDR, 0x21, DRAFT)

  /** The first 64-bits of a murmur3-x64-128 - used for UnixFS directory sharding. */
  case murmur3_x64_64 extends Codec("murmur3-x64-64", HASH, 0x22, PERMANENT)
  case murmur3_32 extends Codec("murmur3-32", HASH, 0x23, DRAFT)
  case ip6 extends Codec("ip6", MULTIADDR, 0x29, PERMANENT)
  case ip6zone extends Codec("ip6zone", MULTIADDR, 0x2a, DRAFT)

  /** CIDR mask for IP addresses */
  case ipcidr extends Codec("ipcidr", MULTIADDR, 0x2b, DRAFT)

  /** Namespace for string paths. Corresponds to `/` in ASCII. */
  case path extends Codec("path", NAMESPACE, 0x2f, PERMANENT)
  case multicodec extends Codec("multicodec", MULTIFORMAT, 0x30, DRAFT)
  case multihash extends Codec("multihash", MULTIFORMAT, 0x31, DRAFT)
  case multiaddr extends Codec("multiaddr", MULTIFORMAT, 0x32, DRAFT)
  case multibase extends Codec("multibase", MULTIFORMAT, 0x33, DRAFT)

  /** Variable signature (varsig) multiformat */
  case varsig extends Codec("varsig", MULTIFORMAT, 0x34, DRAFT)
  case dns extends Codec("dns", MULTIADDR, 0x35, PERMANENT)
  case dns4 extends Codec("dns4", MULTIADDR, 0x36, PERMANENT)
  case dns6 extends Codec("dns6", MULTIADDR, 0x37, PERMANENT)
  case dnsaddr extends Codec("dnsaddr", MULTIADDR, 0x38, PERMANENT)

  /** Protocol Buffers */
  case protobuf extends Codec("protobuf", SERIALIZATION, 0x50, DRAFT)

  /** CBOR */
  case cbor extends Codec("cbor", IPLD, 0x51, PERMANENT)

  /** raw binary */
  case raw extends Codec("raw", IPLD, 0x55, PERMANENT)
  case dbl_sha2_256 extends Codec("dbl-sha2-256", MULTIHASH, 0x56, DRAFT)

  /** recursive length prefix */
  case rlp extends Codec("rlp", SERIALIZATION, 0x60, DRAFT)

  /** bencode */
  case bencode extends Codec("bencode", SERIALIZATION, 0x63, DRAFT)

  /** MerkleDAG protobuf */
  case dag_pb extends Codec("dag-pb", IPLD, 0x70, PERMANENT)

  /** MerkleDAG cbor */
  case dag_cbor extends Codec("dag-cbor", IPLD, 0x71, PERMANENT)

  /** Libp2p Public Key */
  case libp2p_key extends Codec("libp2p-key", IPLD, 0x72, PERMANENT)

  /** Raw Git object */
  case git_raw extends Codec("git-raw", IPLD, 0x78, PERMANENT)

  /** Torrent file info field (bencoded) */
  case torrent_info extends Codec("torrent-info", IPLD, 0x7b, DRAFT)

  /** Torrent file (bencoded) */
  case torrent_file extends Codec("torrent-file", IPLD, 0x7c, DRAFT)

  /** BLAKE3 hash sequence - per Iroh collections spec */
  case blake3_hashseq extends Codec("blake3-hashseq", IPLD, 0x80, DRAFT)

  /** Leofcoin Block */
  case leofcoin_block extends Codec("leofcoin-block", IPLD, 0x81, DRAFT)

  /** Leofcoin Transaction */
  case leofcoin_tx extends Codec("leofcoin-tx", IPLD, 0x82, DRAFT)

  /** Leofcoin Peer Reputation */
  case leofcoin_pr extends Codec("leofcoin-pr", IPLD, 0x83, DRAFT)
  case sctp extends Codec("sctp", MULTIADDR, 0x84, DRAFT)

  /** MerkleDAG JOSE */
  case dag_jose extends Codec("dag-jose", IPLD, 0x85, DRAFT)

  /** MerkleDAG COSE */
  case dag_cose extends Codec("dag-cose", IPLD, 0x86, DRAFT)

  /** LBRY Address */
  case lbry extends Codec("lbry", NAMESPACE, 0x8c, DRAFT)

  /** Ethereum Header (RLP) */
  case eth_block extends Codec("eth-block", IPLD, 0x90, PERMANENT)

  /** Ethereum Header List (RLP) */
  case eth_block_list extends Codec("eth-block-list", IPLD, 0x91, PERMANENT)

  /** Ethereum Transaction Trie (Eth-Trie) */
  case eth_tx_trie extends Codec("eth-tx-trie", IPLD, 0x92, PERMANENT)

  /** Ethereum Transaction (MarshalBinary) */
  case eth_tx extends Codec("eth-tx", IPLD, 0x93, PERMANENT)

  /** Ethereum Transaction Receipt Trie (Eth-Trie) */
  case eth_tx_receipt_trie extends Codec("eth-tx-receipt-trie", IPLD, 0x94, PERMANENT)

  /** Ethereum Transaction Receipt (MarshalBinary) */
  case eth_tx_receipt extends Codec("eth-tx-receipt", IPLD, 0x95, PERMANENT)

  /** Ethereum State Trie (Eth-Secure-Trie) */
  case eth_state_trie extends Codec("eth-state-trie", IPLD, 0x96, PERMANENT)

  /** Ethereum Account Snapshot (RLP) */
  case eth_account_snapshot extends Codec("eth-account-snapshot", IPLD, 0x97, PERMANENT)

  /** Ethereum Contract Storage Trie (Eth-Secure-Trie) */
  case eth_storage_trie extends Codec("eth-storage-trie", IPLD, 0x98, PERMANENT)

  /** Ethereum Transaction Receipt Log Trie (Eth-Trie) */
  case eth_receipt_log_trie extends Codec("eth-receipt-log-trie", IPLD, 0x99, DRAFT)

  /** Ethereum Transaction Receipt Log (RLP) */
  case eth_receipt_log extends Codec("eth-receipt-log", IPLD, 0x9a, DRAFT)

  /** 128-bit AES symmetric key */
  case aes_128 extends Codec("aes-128", KEY, 0xa0, DRAFT)

  /** 192-bit AES symmetric key */
  case aes_192 extends Codec("aes-192", KEY, 0xa1, DRAFT)

  /** 256-bit AES symmetric key */
  case aes_256 extends Codec("aes-256", KEY, 0xa2, DRAFT)

  /** 128-bit ChaCha symmetric key */
  case chacha_128 extends Codec("chacha-128", KEY, 0xa3, DRAFT)

  /** 256-bit ChaCha symmetric key */
  case chacha_256 extends Codec("chacha-256", KEY, 0xa4, DRAFT)

  /** Bitcoin Block */
  case bitcoin_block extends Codec("bitcoin-block", IPLD, 0xb0, PERMANENT)

  /** Bitcoin Tx */
  case bitcoin_tx extends Codec("bitcoin-tx", IPLD, 0xb1, PERMANENT)

  /** Bitcoin Witness Commitment */
  case bitcoin_witness_commitment extends Codec("bitcoin-witness-commitment", IPLD, 0xb2, PERMANENT)

  /** Zcash Block */
  case zcash_block extends Codec("zcash-block", IPLD, 0xc0, PERMANENT)

  /** Zcash Tx */
  case zcash_tx extends Codec("zcash-tx", IPLD, 0xc1, PERMANENT)

  /** CAIP-50 multi-chain account id */
  case caip_50 extends Codec("caip-50", MULTIFORMAT, 0xca, DRAFT)

  /** Ceramic Stream Id */
  case streamid extends Codec("streamid", NAMESPACE, 0xce, DRAFT)

  /** Stellar Block */
  case stellar_block extends Codec("stellar-block", IPLD, 0xd0, DRAFT)

  /** Stellar Tx */
  case stellar_tx extends Codec("stellar-tx", IPLD, 0xd1, DRAFT)
  case md4 extends Codec("md4", MULTIHASH, 0xd4, DRAFT)
  case md5 extends Codec("md5", MULTIHASH, 0xd5, DRAFT)

  /** Decred Block */
  case decred_block extends Codec("decred-block", IPLD, 0xe0, DRAFT)

  /** Decred Tx */
  case decred_tx extends Codec("decred-tx", IPLD, 0xe1, DRAFT)

  /** IPLD path */
  case ipld extends Codec("ipld", NAMESPACE, 0xe2, DRAFT)

  /** IPFS path */
  case ipfs extends Codec("ipfs", NAMESPACE, 0xe3, DRAFT)

  /** Swarm path */
  case swarm extends Codec("swarm", NAMESPACE, 0xe4, DRAFT)

  /** IPNS path */
  case ipns extends Codec("ipns", NAMESPACE, 0xe5, DRAFT)

  /** ZeroNet site address */
  case zeronet extends Codec("zeronet", NAMESPACE, 0xe6, DRAFT)

  /** Secp256k1 public key (compressed) */
  case secp256k1_pub extends Codec("secp256k1-pub", KEY, 0xe7, DRAFT)

  /** DNSLink path */
  case dnslink extends Codec("dnslink", NAMESPACE, 0xe8, PERMANENT)

  /** BLS12-381 public key in the G1 field */
  case bls12_381_g1_pub extends Codec("bls12_381-g1-pub", KEY, 0xea, DRAFT)

  /** BLS12-381 public key in the G2 field */
  case bls12_381_g2_pub extends Codec("bls12_381-g2-pub", KEY, 0xeb, DRAFT)

  /** Curve25519 public key */
  case x25519_pub extends Codec("x25519-pub", KEY, 0xec, DRAFT)

  /** Ed25519 public key */
  case ed25519_pub extends Codec("ed25519-pub", KEY, 0xed, DRAFT)

  /** BLS12-381 concatenated public keys in both the G1 and G2 fields */
  case bls12_381_g1g2_pub extends Codec("bls12_381-g1g2-pub", KEY, 0xee, DRAFT)

  /** Sr25519 public key */
  case sr25519_pub extends Codec("sr25519-pub", KEY, 0xef, DRAFT)

  /** Dash Block */
  case dash_block extends Codec("dash-block", IPLD, 0xf0, DRAFT)

  /** Dash Tx */
  case dash_tx extends Codec("dash-tx", IPLD, 0xf1, DRAFT)

  /** Swarm Manifest */
  case swarm_manifest extends Codec("swarm-manifest", IPLD, 0xfa, DRAFT)

  /** Swarm Feed */
  case swarm_feed extends Codec("swarm-feed", IPLD, 0xfb, DRAFT)

  /** Swarm BeeSon */
  case beeson extends Codec("beeson", IPLD, 0xfc, DRAFT)
  case udp extends Codec("udp", MULTIADDR, 0x0111, DRAFT)

  /** Use webrtc or webrtc-direct instead */
  case p2p_webrtc_star extends Codec("p2p-webrtc-star", MULTIADDR, 0x0113, DEPRECATED)

  /** Use webrtc or webrtc-direct instead */
  case p2p_webrtc_direct extends Codec("p2p-webrtc-direct", MULTIADDR, 0x0114, DEPRECATED)
  case p2p_stardust extends Codec("p2p-stardust", MULTIADDR, 0x0115, DEPRECATED)

  /** ICE-lite webrtc transport with SDP munging during connection establishment and without use of a STUN server */
  case webrtc_direct extends Codec("webrtc-direct", MULTIADDR, 0x0118, DRAFT)

  /** webrtc transport where connection establishment is according to w3c spec */
  case webrtc extends Codec("webrtc", MULTIADDR, 0x0119, DRAFT)
  case p2p_circuit extends Codec("p2p-circuit", MULTIADDR, 0x0122, PERMANENT)

  /** MerkleDAG json */
  case dag_json extends Codec("dag-json", IPLD, 0x0129, PERMANENT)
  case udt extends Codec("udt", MULTIADDR, 0x012d, DRAFT)
  case utp extends Codec("utp", MULTIADDR, 0x012e, DRAFT)

  /** CRC-32 non-cryptographic hash algorithm (IEEE 802.3) */
  case crc32 extends Codec("crc32", HASH, 0x0132, DRAFT)

  /** CRC-64 non-cryptographic hash algorithm (ECMA-182 - Annex B) */
  case crc64_ecma extends Codec("crc64-ecma", HASH, 0x0164, DRAFT)
  case unix extends Codec("unix", MULTIADDR, 0x0190, PERMANENT)

  /** Textile Thread */
  case thread extends Codec("thread", MULTIADDR, 0x0196, DRAFT)

  /** libp2p */
  case p2p extends Codec("p2p", MULTIADDR, 0x01a5, PERMANENT)
  case https extends Codec("https", MULTIADDR, 0x01bb, DRAFT)
  case onion extends Codec("onion", MULTIADDR, 0x01bc, DRAFT)
  case onion3 extends Codec("onion3", MULTIADDR, 0x01bd, DRAFT)

  /** I2P base64 (raw public key) */
  case garlic64 extends Codec("garlic64", MULTIADDR, 0x01be, DRAFT)

  /** I2P base32 (hashed public key or encoded public key/checksum+optional secret) */
  case garlic32 extends Codec("garlic32", MULTIADDR, 0x01bf, DRAFT)
  case tls extends Codec("tls", MULTIADDR, 0x01c0, DRAFT)

  /** Server Name Indication RFC 6066 § 3 */
  case sni extends Codec("sni", MULTIADDR, 0x01c1, DRAFT)
  case noise extends Codec("noise", MULTIADDR, 0x01c6, DRAFT)

  /** Secure Scuttlebutt - Secret Handshake Stream */
  case shs extends Codec("shs", MULTIADDR, 0x01c8, DRAFT)
  case quic extends Codec("quic", MULTIADDR, 0x01cc, PERMANENT)
  case quic_v1 extends Codec("quic-v1", MULTIADDR, 0x01cd, PERMANENT)
  case webtransport extends Codec("webtransport", MULTIADDR, 0x01d1, DRAFT)

  /** TLS certificate's fingerprint as a multihash */
  case certhash extends Codec("certhash", MULTIADDR, 0x01d2, DRAFT)
  case ws extends Codec("ws", MULTIADDR, 0x01dd, PERMANENT)
  case wss extends Codec("wss", MULTIADDR, 0x01de, PERMANENT)
  case p2p_websocket_star extends Codec("p2p-websocket-star", MULTIADDR, 0x01df, PERMANENT)
  case http extends Codec("http", MULTIADDR, 0x01e0, DRAFT)

  /** Percent-encoded path to an HTTP resource */
  case http_path extends Codec("http-path", MULTIADDR, 0x01e1, DRAFT)

  /** SoftWare Heritage persistent IDentifier version 1 snapshot */
  case swhid_1_snp extends Codec("swhid-1-snp", IPLD, 0x01f0, DRAFT)

  /** JSON (UTF-8-encoded) */
  case json extends Codec("json", IPLD, 0x0200, PERMANENT)

  /** MessagePack */
  case messagepack extends Codec("messagepack", SERIALIZATION, 0x0201, DRAFT)

  /** Content Addressable aRchive (CAR) */
  case car extends Codec("car", SERIALIZATION, 0x0202, DRAFT)

  /** Signed IPNS Record */
  case ipns_record extends Codec("ipns-record", SERIALIZATION, 0x0300, PERMANENT)

  /** libp2p peer record type */
  case libp2p_peer_record extends Codec("libp2p-peer-record", LIBP2P, 0x0301, PERMANENT)

  /** libp2p relay reservation voucher */
  case libp2p_relay_rsvp extends Codec("libp2p-relay-rsvp", LIBP2P, 0x0302, PERMANENT)

  /** in memory transport for self-dialing and testing; arbitrary */
  case memorytransport extends Codec("memorytransport", LIBP2P, 0x0309, PERMANENT)

  /** CARv2 IndexSorted index format */
  case car_index_sorted extends Codec("car-index-sorted", SERIALIZATION, 0x0400, DRAFT)

  /** CARv2 MultihashIndexSorted index format */
  case car_multihash_index_sorted extends Codec("car-multihash-index-sorted", SERIALIZATION, 0x0401, DRAFT)

  /** Bitswap datatransfer */
  case transport_bitswap extends Codec("transport-bitswap", TRANSPORT, 0x0900, DRAFT)

  /** Filecoin graphsync datatransfer */
  case transport_graphsync_filecoinv1 extends Codec("transport-graphsync-filecoinv1", TRANSPORT, 0x0910, DRAFT)

  /** HTTP IPFS Gateway trustless datatransfer */
  case transport_ipfs_gateway_http extends Codec("transport-ipfs-gateway-http", TRANSPORT, 0x0920, DRAFT)

  /** Compact encoding for Decentralized Identifers */
  case multidid extends Codec("multidid", MULTIFORMAT, 0x0d1d, DRAFT)

  /** SHA2-256 with the two most significant bits from the last byte zeroed (as via a mask with 0b00111111) - used for
    * proving trees as in Filecoin
    */
  case sha2_256_trunc254_padded extends Codec("sha2-256-trunc254-padded", MULTIHASH, 0x1012, PERMANENT)

  /** aka SHA-224; as specified by FIPS 180-4. */
  case sha2_224 extends Codec("sha2-224", MULTIHASH, 0x1013, PERMANENT)

  /** aka SHA-512/224; as specified by FIPS 180-4. */
  case sha2_512_224 extends Codec("sha2-512-224", MULTIHASH, 0x1014, PERMANENT)

  /** aka SHA-512/256; as specified by FIPS 180-4. */
  case sha2_512_256 extends Codec("sha2-512-256", MULTIHASH, 0x1015, PERMANENT)
  case murmur3_x64_128 extends Codec("murmur3-x64-128", HASH, 0x1022, DRAFT)
  case ripemd_128 extends Codec("ripemd-128", MULTIHASH, 0x1052, DRAFT)
  case ripemd_160 extends Codec("ripemd-160", MULTIHASH, 0x1053, DRAFT)
  case ripemd_256 extends Codec("ripemd-256", MULTIHASH, 0x1054, DRAFT)
  case ripemd_320 extends Codec("ripemd-320", MULTIHASH, 0x1055, DRAFT)
  case x11 extends Codec("x11", MULTIHASH, 0x1100, DRAFT)

  /** P-256 public Key (compressed) */
  case p256_pub extends Codec("p256-pub", KEY, 0x1200, DRAFT)

  /** P-384 public Key (compressed) */
  case p384_pub extends Codec("p384-pub", KEY, 0x1201, DRAFT)

  /** P-521 public Key (compressed) */
  case p521_pub extends Codec("p521-pub", KEY, 0x1202, DRAFT)

  /** Ed448 public Key */
  case ed448_pub extends Codec("ed448-pub", KEY, 0x1203, DRAFT)

  /** X448 public Key */
  case x448_pub extends Codec("x448-pub", KEY, 0x1204, DRAFT)

  /** RSA public key. DER-encoded ASN.1 type RSAPublicKey according to IETF RFC 8017 (PKCS #1) */
  case rsa_pub extends Codec("rsa-pub", KEY, 0x1205, DRAFT)

  /** SM2 public key (compressed) */
  case sm2_pub extends Codec("sm2-pub", KEY, 0x1206, DRAFT)

  /** Verifiable Long-lived ADdress */
  case vlad extends Codec("vlad", VALD, 0x1207, DRAFT)

  /** Verifiable and permissioned append-only log */
  case provenance_log extends Codec("provenance-log", SERIALIZATION, 0x1208, DRAFT)

  /** Verifiable and permissioned append-only log entry */
  case provenance_log_entry extends Codec("provenance-log-entry", SERIALIZATION, 0x1209, DRAFT)

  /** Verifiable and permissioned append-only log script */
  case provenance_log_script extends Codec("provenance-log-script", SERIALIZATION, 0x120a, DRAFT)

  /** ML-KEM 512 public key; as specified by FIPS 203 */
  case mlkem_512_pub extends Codec("mlkem-512-pub", KEY, 0x120b, DRAFT)

  /** ML-KEM 768 public key; as specified by FIPS 203 */
  case mlkem_768_pub extends Codec("mlkem-768-pub", KEY, 0x120c, DRAFT)

  /** ML-KEM 1024 public key; as specified by FIPS 203 */
  case mlkem_1024_pub extends Codec("mlkem-1024-pub", KEY, 0x120d, DRAFT)

  /** Digital signature multiformat */
  case multisig extends Codec("multisig", MULTIFORMAT, 0x1239, DRAFT)

  /** Encryption key multiformat */
  case multikey extends Codec("multikey", MULTIFORMAT, 0x123a, DRAFT)

  /** Nonce random value */
  case nonce extends Codec("nonce", NONCE, 0x123b, DRAFT)

  /** Ed25519 private key */
  case ed25519_priv extends Codec("ed25519-priv", KEY, 0x1300, DRAFT)

  /** Secp256k1 private key */
  case secp256k1_priv extends Codec("secp256k1-priv", KEY, 0x1301, DRAFT)

  /** Curve25519 private key */
  case x25519_priv extends Codec("x25519-priv", KEY, 0x1302, DRAFT)

  /** Sr25519 private key */
  case sr25519_priv extends Codec("sr25519-priv", KEY, 0x1303, DRAFT)

  /** RSA private key */
  case rsa_priv extends Codec("rsa-priv", KEY, 0x1305, DRAFT)

  /** P-256 private key */
  case p256_priv extends Codec("p256-priv", KEY, 0x1306, DRAFT)

  /** P-384 private key */
  case p384_priv extends Codec("p384-priv", KEY, 0x1307, DRAFT)

  /** P-521 private key */
  case p521_priv extends Codec("p521-priv", KEY, 0x1308, DRAFT)

  /** BLS12-381 G1 private key */
  case bls12_381_g1_priv extends Codec("bls12_381-g1-priv", KEY, 0x1309, DRAFT)

  /** BLS12-381 G2 private key */
  case bls12_381_g2_priv extends Codec("bls12_381-g2-priv", KEY, 0x130a, DRAFT)

  /** BLS12-381 G1 and G2 private key */
  case bls12_381_g1g2_priv extends Codec("bls12_381-g1g2-priv", KEY, 0x130b, DRAFT)

  /** BLS12-381 G1 public key share */
  case bls12_381_g1_pub_share extends Codec("bls12_381-g1-pub-share", KEY, 0x130c, DRAFT)

  /** BLS12-381 G2 public key share */
  case bls12_381_g2_pub_share extends Codec("bls12_381-g2-pub-share", KEY, 0x130d, DRAFT)

  /** BLS12-381 G1 private key share */
  case bls12_381_g1_priv_share extends Codec("bls12_381-g1-priv-share", KEY, 0x130e, DRAFT)

  /** BLS12-381 G2 private key share */
  case bls12_381_g2_priv_share extends Codec("bls12_381-g2-priv-share", KEY, 0x130f, DRAFT)

  /** Lamport public key based on SHA3-512 */
  case lamport_sha3_512_pub extends Codec("lamport-sha3-512-pub", KEY, 0x1a14, DRAFT)

  /** Lamport public key based on SHA3-384 */
  case lamport_sha3_384_pub extends Codec("lamport-sha3-384-pub", KEY, 0x1a15, DRAFT)

  /** Lamport public key based on SHA3-256 */
  case lamport_sha3_256_pub extends Codec("lamport-sha3-256-pub", KEY, 0x1a16, DRAFT)

  /** Lamport private key based on SHA3-512 */
  case lamport_sha3_512_priv extends Codec("lamport-sha3-512-priv", KEY, 0x1a24, DRAFT)

  /** Lamport private key based on SHA3-384 */
  case lamport_sha3_384_priv extends Codec("lamport-sha3-384-priv", KEY, 0x1a25, DRAFT)

  /** Lamport private key based on SHA3-256 */
  case lamport_sha3_256_priv extends Codec("lamport-sha3-256-priv", KEY, 0x1a26, DRAFT)

  /** Lamport private key share based on SHA3-512 and split with Shamir gf256 */
  case lamport_sha3_512_priv_share extends Codec("lamport-sha3-512-priv-share", KEY, 0x1a34, DRAFT)

  /** Lamport private key share based on SHA3-384 and split with Shamir gf256 */
  case lamport_sha3_384_priv_share extends Codec("lamport-sha3-384-priv-share", KEY, 0x1a35, DRAFT)

  /** Lamport private key share based on SHA3-256 and split with Shamir gf256 */
  case lamport_sha3_256_priv_share extends Codec("lamport-sha3-256-priv-share", KEY, 0x1a36, DRAFT)

  /** Lamport signature based on SHA3-512 */
  case lamport_sha3_512_sig extends Codec("lamport-sha3-512-sig", MULTISIG, 0x1a44, DRAFT)

  /** Lamport signature based on SHA3-384 */
  case lamport_sha3_384_sig extends Codec("lamport-sha3-384-sig", MULTISIG, 0x1a45, DRAFT)

  /** Lamport signature based on SHA3-256 */
  case lamport_sha3_256_sig extends Codec("lamport-sha3-256-sig", MULTISIG, 0x1a46, DRAFT)

  /** Lamport signature share based on SHA3-512 and split with Shamir gf256 */
  case lamport_sha3_512_sig_share extends Codec("lamport-sha3-512-sig-share", MULTISIG, 0x1a54, DRAFT)

  /** Lamport signature share based on SHA3-384 and split with Shamir gf256 */
  case lamport_sha3_384_sig_share extends Codec("lamport-sha3-384-sig-share", MULTISIG, 0x1a55, DRAFT)

  /** Lamport signature share based on SHA3-256 and split with Shamir gf256 */
  case lamport_sha3_256_sig_share extends Codec("lamport-sha3-256-sig-share", MULTISIG, 0x1a56, DRAFT)

  /** KangarooTwelve is an extendable-output hash function based on Keccak-p */
  case kangarootwelve extends Codec("kangarootwelve", MULTIHASH, 0x1d01, DRAFT)

  /** AES Galois/Counter Mode with 256-bit key and 12-byte IV */
  case aes_gcm_256 extends Codec("aes-gcm-256", ENCRYPTION, 0x2000, DRAFT)

  /** Experimental QUIC over yggdrasil and ironwood routing protocol */
  case silverpine extends Codec("silverpine", MULTIADDR, 0x3f42, DRAFT)
  case sm3_256 extends Codec("sm3-256", MULTIHASH, 0x534d, DRAFT)

  /** The sum of multiple sha2-256 hashes; as specified by Ceramic CIP-124. */
  case sha256a extends Codec("sha256a", HASH, 0x7012, DRAFT)

  /** ChaCha20_Poly1305 encryption scheme */
  case chacha20_poly1305 extends Codec("chacha20-poly1305", MULTIKEY, 0xa000, DRAFT)

  /** Blake2b consists of 64 output lengths that give different hashes */
  case blake2b_8 extends Codec("blake2b-8", MULTIHASH, 0xb201, DRAFT)
  case blake2b_16 extends Codec("blake2b-16", MULTIHASH, 0xb202, DRAFT)
  case blake2b_24 extends Codec("blake2b-24", MULTIHASH, 0xb203, DRAFT)
  case blake2b_32 extends Codec("blake2b-32", MULTIHASH, 0xb204, DRAFT)
  case blake2b_40 extends Codec("blake2b-40", MULTIHASH, 0xb205, DRAFT)
  case blake2b_48 extends Codec("blake2b-48", MULTIHASH, 0xb206, DRAFT)
  case blake2b_56 extends Codec("blake2b-56", MULTIHASH, 0xb207, DRAFT)
  case blake2b_64 extends Codec("blake2b-64", MULTIHASH, 0xb208, DRAFT)
  case blake2b_72 extends Codec("blake2b-72", MULTIHASH, 0xb209, DRAFT)
  case blake2b_80 extends Codec("blake2b-80", MULTIHASH, 0xb20a, DRAFT)
  case blake2b_88 extends Codec("blake2b-88", MULTIHASH, 0xb20b, DRAFT)
  case blake2b_96 extends Codec("blake2b-96", MULTIHASH, 0xb20c, DRAFT)
  case blake2b_104 extends Codec("blake2b-104", MULTIHASH, 0xb20d, DRAFT)
  case blake2b_112 extends Codec("blake2b-112", MULTIHASH, 0xb20e, DRAFT)
  case blake2b_120 extends Codec("blake2b-120", MULTIHASH, 0xb20f, DRAFT)
  case blake2b_128 extends Codec("blake2b-128", MULTIHASH, 0xb210, DRAFT)
  case blake2b_136 extends Codec("blake2b-136", MULTIHASH, 0xb211, DRAFT)
  case blake2b_144 extends Codec("blake2b-144", MULTIHASH, 0xb212, DRAFT)
  case blake2b_152 extends Codec("blake2b-152", MULTIHASH, 0xb213, DRAFT)
  case blake2b_160 extends Codec("blake2b-160", MULTIHASH, 0xb214, DRAFT)
  case blake2b_168 extends Codec("blake2b-168", MULTIHASH, 0xb215, DRAFT)
  case blake2b_176 extends Codec("blake2b-176", MULTIHASH, 0xb216, DRAFT)
  case blake2b_184 extends Codec("blake2b-184", MULTIHASH, 0xb217, DRAFT)
  case blake2b_192 extends Codec("blake2b-192", MULTIHASH, 0xb218, DRAFT)
  case blake2b_200 extends Codec("blake2b-200", MULTIHASH, 0xb219, DRAFT)
  case blake2b_208 extends Codec("blake2b-208", MULTIHASH, 0xb21a, DRAFT)
  case blake2b_216 extends Codec("blake2b-216", MULTIHASH, 0xb21b, DRAFT)
  case blake2b_224 extends Codec("blake2b-224", MULTIHASH, 0xb21c, DRAFT)
  case blake2b_232 extends Codec("blake2b-232", MULTIHASH, 0xb21d, DRAFT)
  case blake2b_240 extends Codec("blake2b-240", MULTIHASH, 0xb21e, DRAFT)
  case blake2b_248 extends Codec("blake2b-248", MULTIHASH, 0xb21f, DRAFT)
  case blake2b_256 extends Codec("blake2b-256", MULTIHASH, 0xb220, PERMANENT)
  case blake2b_264 extends Codec("blake2b-264", MULTIHASH, 0xb221, DRAFT)
  case blake2b_272 extends Codec("blake2b-272", MULTIHASH, 0xb222, DRAFT)
  case blake2b_280 extends Codec("blake2b-280", MULTIHASH, 0xb223, DRAFT)
  case blake2b_288 extends Codec("blake2b-288", MULTIHASH, 0xb224, DRAFT)
  case blake2b_296 extends Codec("blake2b-296", MULTIHASH, 0xb225, DRAFT)
  case blake2b_304 extends Codec("blake2b-304", MULTIHASH, 0xb226, DRAFT)
  case blake2b_312 extends Codec("blake2b-312", MULTIHASH, 0xb227, DRAFT)
  case blake2b_320 extends Codec("blake2b-320", MULTIHASH, 0xb228, DRAFT)
  case blake2b_328 extends Codec("blake2b-328", MULTIHASH, 0xb229, DRAFT)
  case blake2b_336 extends Codec("blake2b-336", MULTIHASH, 0xb22a, DRAFT)
  case blake2b_344 extends Codec("blake2b-344", MULTIHASH, 0xb22b, DRAFT)
  case blake2b_352 extends Codec("blake2b-352", MULTIHASH, 0xb22c, DRAFT)
  case blake2b_360 extends Codec("blake2b-360", MULTIHASH, 0xb22d, DRAFT)
  case blake2b_368 extends Codec("blake2b-368", MULTIHASH, 0xb22e, DRAFT)
  case blake2b_376 extends Codec("blake2b-376", MULTIHASH, 0xb22f, DRAFT)
  case blake2b_384 extends Codec("blake2b-384", MULTIHASH, 0xb230, DRAFT)
  case blake2b_392 extends Codec("blake2b-392", MULTIHASH, 0xb231, DRAFT)
  case blake2b_400 extends Codec("blake2b-400", MULTIHASH, 0xb232, DRAFT)
  case blake2b_408 extends Codec("blake2b-408", MULTIHASH, 0xb233, DRAFT)
  case blake2b_416 extends Codec("blake2b-416", MULTIHASH, 0xb234, DRAFT)
  case blake2b_424 extends Codec("blake2b-424", MULTIHASH, 0xb235, DRAFT)
  case blake2b_432 extends Codec("blake2b-432", MULTIHASH, 0xb236, DRAFT)
  case blake2b_440 extends Codec("blake2b-440", MULTIHASH, 0xb237, DRAFT)
  case blake2b_448 extends Codec("blake2b-448", MULTIHASH, 0xb238, DRAFT)
  case blake2b_456 extends Codec("blake2b-456", MULTIHASH, 0xb239, DRAFT)
  case blake2b_464 extends Codec("blake2b-464", MULTIHASH, 0xb23a, DRAFT)
  case blake2b_472 extends Codec("blake2b-472", MULTIHASH, 0xb23b, DRAFT)
  case blake2b_480 extends Codec("blake2b-480", MULTIHASH, 0xb23c, DRAFT)
  case blake2b_488 extends Codec("blake2b-488", MULTIHASH, 0xb23d, DRAFT)
  case blake2b_496 extends Codec("blake2b-496", MULTIHASH, 0xb23e, DRAFT)
  case blake2b_504 extends Codec("blake2b-504", MULTIHASH, 0xb23f, DRAFT)
  case blake2b_512 extends Codec("blake2b-512", MULTIHASH, 0xb240, DRAFT)

  /** Blake2s consists of 32 output lengths that give different hashes */
  case blake2s_8 extends Codec("blake2s-8", MULTIHASH, 0xb241, DRAFT)
  case blake2s_16 extends Codec("blake2s-16", MULTIHASH, 0xb242, DRAFT)
  case blake2s_24 extends Codec("blake2s-24", MULTIHASH, 0xb243, DRAFT)
  case blake2s_32 extends Codec("blake2s-32", MULTIHASH, 0xb244, DRAFT)
  case blake2s_40 extends Codec("blake2s-40", MULTIHASH, 0xb245, DRAFT)
  case blake2s_48 extends Codec("blake2s-48", MULTIHASH, 0xb246, DRAFT)
  case blake2s_56 extends Codec("blake2s-56", MULTIHASH, 0xb247, DRAFT)
  case blake2s_64 extends Codec("blake2s-64", MULTIHASH, 0xb248, DRAFT)
  case blake2s_72 extends Codec("blake2s-72", MULTIHASH, 0xb249, DRAFT)
  case blake2s_80 extends Codec("blake2s-80", MULTIHASH, 0xb24a, DRAFT)
  case blake2s_88 extends Codec("blake2s-88", MULTIHASH, 0xb24b, DRAFT)
  case blake2s_96 extends Codec("blake2s-96", MULTIHASH, 0xb24c, DRAFT)
  case blake2s_104 extends Codec("blake2s-104", MULTIHASH, 0xb24d, DRAFT)
  case blake2s_112 extends Codec("blake2s-112", MULTIHASH, 0xb24e, DRAFT)
  case blake2s_120 extends Codec("blake2s-120", MULTIHASH, 0xb24f, DRAFT)
  case blake2s_128 extends Codec("blake2s-128", MULTIHASH, 0xb250, DRAFT)
  case blake2s_136 extends Codec("blake2s-136", MULTIHASH, 0xb251, DRAFT)
  case blake2s_144 extends Codec("blake2s-144", MULTIHASH, 0xb252, DRAFT)
  case blake2s_152 extends Codec("blake2s-152", MULTIHASH, 0xb253, DRAFT)
  case blake2s_160 extends Codec("blake2s-160", MULTIHASH, 0xb254, DRAFT)
  case blake2s_168 extends Codec("blake2s-168", MULTIHASH, 0xb255, DRAFT)
  case blake2s_176 extends Codec("blake2s-176", MULTIHASH, 0xb256, DRAFT)
  case blake2s_184 extends Codec("blake2s-184", MULTIHASH, 0xb257, DRAFT)
  case blake2s_192 extends Codec("blake2s-192", MULTIHASH, 0xb258, DRAFT)
  case blake2s_200 extends Codec("blake2s-200", MULTIHASH, 0xb259, DRAFT)
  case blake2s_208 extends Codec("blake2s-208", MULTIHASH, 0xb25a, DRAFT)
  case blake2s_216 extends Codec("blake2s-216", MULTIHASH, 0xb25b, DRAFT)
  case blake2s_224 extends Codec("blake2s-224", MULTIHASH, 0xb25c, DRAFT)
  case blake2s_232 extends Codec("blake2s-232", MULTIHASH, 0xb25d, DRAFT)
  case blake2s_240 extends Codec("blake2s-240", MULTIHASH, 0xb25e, DRAFT)
  case blake2s_248 extends Codec("blake2s-248", MULTIHASH, 0xb25f, DRAFT)
  case blake2s_256 extends Codec("blake2s-256", MULTIHASH, 0xb260, DRAFT)

  /** Skein256 consists of 32 output lengths that give different hashes */
  case skein256_8 extends Codec("skein256-8", MULTIHASH, 0xb301, DRAFT)
  case skein256_16 extends Codec("skein256-16", MULTIHASH, 0xb302, DRAFT)
  case skein256_24 extends Codec("skein256-24", MULTIHASH, 0xb303, DRAFT)
  case skein256_32 extends Codec("skein256-32", MULTIHASH, 0xb304, DRAFT)
  case skein256_40 extends Codec("skein256-40", MULTIHASH, 0xb305, DRAFT)
  case skein256_48 extends Codec("skein256-48", MULTIHASH, 0xb306, DRAFT)
  case skein256_56 extends Codec("skein256-56", MULTIHASH, 0xb307, DRAFT)
  case skein256_64 extends Codec("skein256-64", MULTIHASH, 0xb308, DRAFT)
  case skein256_72 extends Codec("skein256-72", MULTIHASH, 0xb309, DRAFT)
  case skein256_80 extends Codec("skein256-80", MULTIHASH, 0xb30a, DRAFT)
  case skein256_88 extends Codec("skein256-88", MULTIHASH, 0xb30b, DRAFT)
  case skein256_96 extends Codec("skein256-96", MULTIHASH, 0xb30c, DRAFT)
  case skein256_104 extends Codec("skein256-104", MULTIHASH, 0xb30d, DRAFT)
  case skein256_112 extends Codec("skein256-112", MULTIHASH, 0xb30e, DRAFT)
  case skein256_120 extends Codec("skein256-120", MULTIHASH, 0xb30f, DRAFT)
  case skein256_128 extends Codec("skein256-128", MULTIHASH, 0xb310, DRAFT)
  case skein256_136 extends Codec("skein256-136", MULTIHASH, 0xb311, DRAFT)
  case skein256_144 extends Codec("skein256-144", MULTIHASH, 0xb312, DRAFT)
  case skein256_152 extends Codec("skein256-152", MULTIHASH, 0xb313, DRAFT)
  case skein256_160 extends Codec("skein256-160", MULTIHASH, 0xb314, DRAFT)
  case skein256_168 extends Codec("skein256-168", MULTIHASH, 0xb315, DRAFT)
  case skein256_176 extends Codec("skein256-176", MULTIHASH, 0xb316, DRAFT)
  case skein256_184 extends Codec("skein256-184", MULTIHASH, 0xb317, DRAFT)
  case skein256_192 extends Codec("skein256-192", MULTIHASH, 0xb318, DRAFT)
  case skein256_200 extends Codec("skein256-200", MULTIHASH, 0xb319, DRAFT)
  case skein256_208 extends Codec("skein256-208", MULTIHASH, 0xb31a, DRAFT)
  case skein256_216 extends Codec("skein256-216", MULTIHASH, 0xb31b, DRAFT)
  case skein256_224 extends Codec("skein256-224", MULTIHASH, 0xb31c, DRAFT)
  case skein256_232 extends Codec("skein256-232", MULTIHASH, 0xb31d, DRAFT)
  case skein256_240 extends Codec("skein256-240", MULTIHASH, 0xb31e, DRAFT)
  case skein256_248 extends Codec("skein256-248", MULTIHASH, 0xb31f, DRAFT)
  case skein256_256 extends Codec("skein256-256", MULTIHASH, 0xb320, DRAFT)

  /** Skein512 consists of 64 output lengths that give different hashes */
  case skein512_8 extends Codec("skein512-8", MULTIHASH, 0xb321, DRAFT)
  case skein512_16 extends Codec("skein512-16", MULTIHASH, 0xb322, DRAFT)
  case skein512_24 extends Codec("skein512-24", MULTIHASH, 0xb323, DRAFT)
  case skein512_32 extends Codec("skein512-32", MULTIHASH, 0xb324, DRAFT)
  case skein512_40 extends Codec("skein512-40", MULTIHASH, 0xb325, DRAFT)
  case skein512_48 extends Codec("skein512-48", MULTIHASH, 0xb326, DRAFT)
  case skein512_56 extends Codec("skein512-56", MULTIHASH, 0xb327, DRAFT)
  case skein512_64 extends Codec("skein512-64", MULTIHASH, 0xb328, DRAFT)
  case skein512_72 extends Codec("skein512-72", MULTIHASH, 0xb329, DRAFT)
  case skein512_80 extends Codec("skein512-80", MULTIHASH, 0xb32a, DRAFT)
  case skein512_88 extends Codec("skein512-88", MULTIHASH, 0xb32b, DRAFT)
  case skein512_96 extends Codec("skein512-96", MULTIHASH, 0xb32c, DRAFT)
  case skein512_104 extends Codec("skein512-104", MULTIHASH, 0xb32d, DRAFT)
  case skein512_112 extends Codec("skein512-112", MULTIHASH, 0xb32e, DRAFT)
  case skein512_120 extends Codec("skein512-120", MULTIHASH, 0xb32f, DRAFT)
  case skein512_128 extends Codec("skein512-128", MULTIHASH, 0xb330, DRAFT)
  case skein512_136 extends Codec("skein512-136", MULTIHASH, 0xb331, DRAFT)
  case skein512_144 extends Codec("skein512-144", MULTIHASH, 0xb332, DRAFT)
  case skein512_152 extends Codec("skein512-152", MULTIHASH, 0xb333, DRAFT)
  case skein512_160 extends Codec("skein512-160", MULTIHASH, 0xb334, DRAFT)
  case skein512_168 extends Codec("skein512-168", MULTIHASH, 0xb335, DRAFT)
  case skein512_176 extends Codec("skein512-176", MULTIHASH, 0xb336, DRAFT)
  case skein512_184 extends Codec("skein512-184", MULTIHASH, 0xb337, DRAFT)
  case skein512_192 extends Codec("skein512-192", MULTIHASH, 0xb338, DRAFT)
  case skein512_200 extends Codec("skein512-200", MULTIHASH, 0xb339, DRAFT)
  case skein512_208 extends Codec("skein512-208", MULTIHASH, 0xb33a, DRAFT)
  case skein512_216 extends Codec("skein512-216", MULTIHASH, 0xb33b, DRAFT)
  case skein512_224 extends Codec("skein512-224", MULTIHASH, 0xb33c, DRAFT)
  case skein512_232 extends Codec("skein512-232", MULTIHASH, 0xb33d, DRAFT)
  case skein512_240 extends Codec("skein512-240", MULTIHASH, 0xb33e, DRAFT)
  case skein512_248 extends Codec("skein512-248", MULTIHASH, 0xb33f, DRAFT)
  case skein512_256 extends Codec("skein512-256", MULTIHASH, 0xb340, DRAFT)
  case skein512_264 extends Codec("skein512-264", MULTIHASH, 0xb341, DRAFT)
  case skein512_272 extends Codec("skein512-272", MULTIHASH, 0xb342, DRAFT)
  case skein512_280 extends Codec("skein512-280", MULTIHASH, 0xb343, DRAFT)
  case skein512_288 extends Codec("skein512-288", MULTIHASH, 0xb344, DRAFT)
  case skein512_296 extends Codec("skein512-296", MULTIHASH, 0xb345, DRAFT)
  case skein512_304 extends Codec("skein512-304", MULTIHASH, 0xb346, DRAFT)
  case skein512_312 extends Codec("skein512-312", MULTIHASH, 0xb347, DRAFT)
  case skein512_320 extends Codec("skein512-320", MULTIHASH, 0xb348, DRAFT)
  case skein512_328 extends Codec("skein512-328", MULTIHASH, 0xb349, DRAFT)
  case skein512_336 extends Codec("skein512-336", MULTIHASH, 0xb34a, DRAFT)
  case skein512_344 extends Codec("skein512-344", MULTIHASH, 0xb34b, DRAFT)
  case skein512_352 extends Codec("skein512-352", MULTIHASH, 0xb34c, DRAFT)
  case skein512_360 extends Codec("skein512-360", MULTIHASH, 0xb34d, DRAFT)
  case skein512_368 extends Codec("skein512-368", MULTIHASH, 0xb34e, DRAFT)
  case skein512_376 extends Codec("skein512-376", MULTIHASH, 0xb34f, DRAFT)
  case skein512_384 extends Codec("skein512-384", MULTIHASH, 0xb350, DRAFT)
  case skein512_392 extends Codec("skein512-392", MULTIHASH, 0xb351, DRAFT)
  case skein512_400 extends Codec("skein512-400", MULTIHASH, 0xb352, DRAFT)
  case skein512_408 extends Codec("skein512-408", MULTIHASH, 0xb353, DRAFT)
  case skein512_416 extends Codec("skein512-416", MULTIHASH, 0xb354, DRAFT)
  case skein512_424 extends Codec("skein512-424", MULTIHASH, 0xb355, DRAFT)
  case skein512_432 extends Codec("skein512-432", MULTIHASH, 0xb356, DRAFT)
  case skein512_440 extends Codec("skein512-440", MULTIHASH, 0xb357, DRAFT)
  case skein512_448 extends Codec("skein512-448", MULTIHASH, 0xb358, DRAFT)
  case skein512_456 extends Codec("skein512-456", MULTIHASH, 0xb359, DRAFT)
  case skein512_464 extends Codec("skein512-464", MULTIHASH, 0xb35a, DRAFT)
  case skein512_472 extends Codec("skein512-472", MULTIHASH, 0xb35b, DRAFT)
  case skein512_480 extends Codec("skein512-480", MULTIHASH, 0xb35c, DRAFT)
  case skein512_488 extends Codec("skein512-488", MULTIHASH, 0xb35d, DRAFT)
  case skein512_496 extends Codec("skein512-496", MULTIHASH, 0xb35e, DRAFT)
  case skein512_504 extends Codec("skein512-504", MULTIHASH, 0xb35f, DRAFT)
  case skein512_512 extends Codec("skein512-512", MULTIHASH, 0xb360, DRAFT)

  /** Skein1024 consists of 128 output lengths that give different hashes */
  case skein1024_8 extends Codec("skein1024-8", MULTIHASH, 0xb361, DRAFT)
  case skein1024_16 extends Codec("skein1024-16", MULTIHASH, 0xb362, DRAFT)
  case skein1024_24 extends Codec("skein1024-24", MULTIHASH, 0xb363, DRAFT)
  case skein1024_32 extends Codec("skein1024-32", MULTIHASH, 0xb364, DRAFT)
  case skein1024_40 extends Codec("skein1024-40", MULTIHASH, 0xb365, DRAFT)
  case skein1024_48 extends Codec("skein1024-48", MULTIHASH, 0xb366, DRAFT)
  case skein1024_56 extends Codec("skein1024-56", MULTIHASH, 0xb367, DRAFT)
  case skein1024_64 extends Codec("skein1024-64", MULTIHASH, 0xb368, DRAFT)
  case skein1024_72 extends Codec("skein1024-72", MULTIHASH, 0xb369, DRAFT)
  case skein1024_80 extends Codec("skein1024-80", MULTIHASH, 0xb36a, DRAFT)
  case skein1024_88 extends Codec("skein1024-88", MULTIHASH, 0xb36b, DRAFT)
  case skein1024_96 extends Codec("skein1024-96", MULTIHASH, 0xb36c, DRAFT)
  case skein1024_104 extends Codec("skein1024-104", MULTIHASH, 0xb36d, DRAFT)
  case skein1024_112 extends Codec("skein1024-112", MULTIHASH, 0xb36e, DRAFT)
  case skein1024_120 extends Codec("skein1024-120", MULTIHASH, 0xb36f, DRAFT)
  case skein1024_128 extends Codec("skein1024-128", MULTIHASH, 0xb370, DRAFT)
  case skein1024_136 extends Codec("skein1024-136", MULTIHASH, 0xb371, DRAFT)
  case skein1024_144 extends Codec("skein1024-144", MULTIHASH, 0xb372, DRAFT)
  case skein1024_152 extends Codec("skein1024-152", MULTIHASH, 0xb373, DRAFT)
  case skein1024_160 extends Codec("skein1024-160", MULTIHASH, 0xb374, DRAFT)
  case skein1024_168 extends Codec("skein1024-168", MULTIHASH, 0xb375, DRAFT)
  case skein1024_176 extends Codec("skein1024-176", MULTIHASH, 0xb376, DRAFT)
  case skein1024_184 extends Codec("skein1024-184", MULTIHASH, 0xb377, DRAFT)
  case skein1024_192 extends Codec("skein1024-192", MULTIHASH, 0xb378, DRAFT)
  case skein1024_200 extends Codec("skein1024-200", MULTIHASH, 0xb379, DRAFT)
  case skein1024_208 extends Codec("skein1024-208", MULTIHASH, 0xb37a, DRAFT)
  case skein1024_216 extends Codec("skein1024-216", MULTIHASH, 0xb37b, DRAFT)
  case skein1024_224 extends Codec("skein1024-224", MULTIHASH, 0xb37c, DRAFT)
  case skein1024_232 extends Codec("skein1024-232", MULTIHASH, 0xb37d, DRAFT)
  case skein1024_240 extends Codec("skein1024-240", MULTIHASH, 0xb37e, DRAFT)
  case skein1024_248 extends Codec("skein1024-248", MULTIHASH, 0xb37f, DRAFT)
  case skein1024_256 extends Codec("skein1024-256", MULTIHASH, 0xb380, DRAFT)
  case skein1024_264 extends Codec("skein1024-264", MULTIHASH, 0xb381, DRAFT)
  case skein1024_272 extends Codec("skein1024-272", MULTIHASH, 0xb382, DRAFT)
  case skein1024_280 extends Codec("skein1024-280", MULTIHASH, 0xb383, DRAFT)
  case skein1024_288 extends Codec("skein1024-288", MULTIHASH, 0xb384, DRAFT)
  case skein1024_296 extends Codec("skein1024-296", MULTIHASH, 0xb385, DRAFT)
  case skein1024_304 extends Codec("skein1024-304", MULTIHASH, 0xb386, DRAFT)
  case skein1024_312 extends Codec("skein1024-312", MULTIHASH, 0xb387, DRAFT)
  case skein1024_320 extends Codec("skein1024-320", MULTIHASH, 0xb388, DRAFT)
  case skein1024_328 extends Codec("skein1024-328", MULTIHASH, 0xb389, DRAFT)
  case skein1024_336 extends Codec("skein1024-336", MULTIHASH, 0xb38a, DRAFT)
  case skein1024_344 extends Codec("skein1024-344", MULTIHASH, 0xb38b, DRAFT)
  case skein1024_352 extends Codec("skein1024-352", MULTIHASH, 0xb38c, DRAFT)
  case skein1024_360 extends Codec("skein1024-360", MULTIHASH, 0xb38d, DRAFT)
  case skein1024_368 extends Codec("skein1024-368", MULTIHASH, 0xb38e, DRAFT)
  case skein1024_376 extends Codec("skein1024-376", MULTIHASH, 0xb38f, DRAFT)
  case skein1024_384 extends Codec("skein1024-384", MULTIHASH, 0xb390, DRAFT)
  case skein1024_392 extends Codec("skein1024-392", MULTIHASH, 0xb391, DRAFT)
  case skein1024_400 extends Codec("skein1024-400", MULTIHASH, 0xb392, DRAFT)
  case skein1024_408 extends Codec("skein1024-408", MULTIHASH, 0xb393, DRAFT)
  case skein1024_416 extends Codec("skein1024-416", MULTIHASH, 0xb394, DRAFT)
  case skein1024_424 extends Codec("skein1024-424", MULTIHASH, 0xb395, DRAFT)
  case skein1024_432 extends Codec("skein1024-432", MULTIHASH, 0xb396, DRAFT)
  case skein1024_440 extends Codec("skein1024-440", MULTIHASH, 0xb397, DRAFT)
  case skein1024_448 extends Codec("skein1024-448", MULTIHASH, 0xb398, DRAFT)
  case skein1024_456 extends Codec("skein1024-456", MULTIHASH, 0xb399, DRAFT)
  case skein1024_464 extends Codec("skein1024-464", MULTIHASH, 0xb39a, DRAFT)
  case skein1024_472 extends Codec("skein1024-472", MULTIHASH, 0xb39b, DRAFT)
  case skein1024_480 extends Codec("skein1024-480", MULTIHASH, 0xb39c, DRAFT)
  case skein1024_488 extends Codec("skein1024-488", MULTIHASH, 0xb39d, DRAFT)
  case skein1024_496 extends Codec("skein1024-496", MULTIHASH, 0xb39e, DRAFT)
  case skein1024_504 extends Codec("skein1024-504", MULTIHASH, 0xb39f, DRAFT)
  case skein1024_512 extends Codec("skein1024-512", MULTIHASH, 0xb3a0, DRAFT)
  case skein1024_520 extends Codec("skein1024-520", MULTIHASH, 0xb3a1, DRAFT)
  case skein1024_528 extends Codec("skein1024-528", MULTIHASH, 0xb3a2, DRAFT)
  case skein1024_536 extends Codec("skein1024-536", MULTIHASH, 0xb3a3, DRAFT)
  case skein1024_544 extends Codec("skein1024-544", MULTIHASH, 0xb3a4, DRAFT)
  case skein1024_552 extends Codec("skein1024-552", MULTIHASH, 0xb3a5, DRAFT)
  case skein1024_560 extends Codec("skein1024-560", MULTIHASH, 0xb3a6, DRAFT)
  case skein1024_568 extends Codec("skein1024-568", MULTIHASH, 0xb3a7, DRAFT)
  case skein1024_576 extends Codec("skein1024-576", MULTIHASH, 0xb3a8, DRAFT)
  case skein1024_584 extends Codec("skein1024-584", MULTIHASH, 0xb3a9, DRAFT)
  case skein1024_592 extends Codec("skein1024-592", MULTIHASH, 0xb3aa, DRAFT)
  case skein1024_600 extends Codec("skein1024-600", MULTIHASH, 0xb3ab, DRAFT)
  case skein1024_608 extends Codec("skein1024-608", MULTIHASH, 0xb3ac, DRAFT)
  case skein1024_616 extends Codec("skein1024-616", MULTIHASH, 0xb3ad, DRAFT)
  case skein1024_624 extends Codec("skein1024-624", MULTIHASH, 0xb3ae, DRAFT)
  case skein1024_632 extends Codec("skein1024-632", MULTIHASH, 0xb3af, DRAFT)
  case skein1024_640 extends Codec("skein1024-640", MULTIHASH, 0xb3b0, DRAFT)
  case skein1024_648 extends Codec("skein1024-648", MULTIHASH, 0xb3b1, DRAFT)
  case skein1024_656 extends Codec("skein1024-656", MULTIHASH, 0xb3b2, DRAFT)
  case skein1024_664 extends Codec("skein1024-664", MULTIHASH, 0xb3b3, DRAFT)
  case skein1024_672 extends Codec("skein1024-672", MULTIHASH, 0xb3b4, DRAFT)
  case skein1024_680 extends Codec("skein1024-680", MULTIHASH, 0xb3b5, DRAFT)
  case skein1024_688 extends Codec("skein1024-688", MULTIHASH, 0xb3b6, DRAFT)
  case skein1024_696 extends Codec("skein1024-696", MULTIHASH, 0xb3b7, DRAFT)
  case skein1024_704 extends Codec("skein1024-704", MULTIHASH, 0xb3b8, DRAFT)
  case skein1024_712 extends Codec("skein1024-712", MULTIHASH, 0xb3b9, DRAFT)
  case skein1024_720 extends Codec("skein1024-720", MULTIHASH, 0xb3ba, DRAFT)
  case skein1024_728 extends Codec("skein1024-728", MULTIHASH, 0xb3bb, DRAFT)
  case skein1024_736 extends Codec("skein1024-736", MULTIHASH, 0xb3bc, DRAFT)
  case skein1024_744 extends Codec("skein1024-744", MULTIHASH, 0xb3bd, DRAFT)
  case skein1024_752 extends Codec("skein1024-752", MULTIHASH, 0xb3be, DRAFT)
  case skein1024_760 extends Codec("skein1024-760", MULTIHASH, 0xb3bf, DRAFT)
  case skein1024_768 extends Codec("skein1024-768", MULTIHASH, 0xb3c0, DRAFT)
  case skein1024_776 extends Codec("skein1024-776", MULTIHASH, 0xb3c1, DRAFT)
  case skein1024_784 extends Codec("skein1024-784", MULTIHASH, 0xb3c2, DRAFT)
  case skein1024_792 extends Codec("skein1024-792", MULTIHASH, 0xb3c3, DRAFT)
  case skein1024_800 extends Codec("skein1024-800", MULTIHASH, 0xb3c4, DRAFT)
  case skein1024_808 extends Codec("skein1024-808", MULTIHASH, 0xb3c5, DRAFT)
  case skein1024_816 extends Codec("skein1024-816", MULTIHASH, 0xb3c6, DRAFT)
  case skein1024_824 extends Codec("skein1024-824", MULTIHASH, 0xb3c7, DRAFT)
  case skein1024_832 extends Codec("skein1024-832", MULTIHASH, 0xb3c8, DRAFT)
  case skein1024_840 extends Codec("skein1024-840", MULTIHASH, 0xb3c9, DRAFT)
  case skein1024_848 extends Codec("skein1024-848", MULTIHASH, 0xb3ca, DRAFT)
  case skein1024_856 extends Codec("skein1024-856", MULTIHASH, 0xb3cb, DRAFT)
  case skein1024_864 extends Codec("skein1024-864", MULTIHASH, 0xb3cc, DRAFT)
  case skein1024_872 extends Codec("skein1024-872", MULTIHASH, 0xb3cd, DRAFT)
  case skein1024_880 extends Codec("skein1024-880", MULTIHASH, 0xb3ce, DRAFT)
  case skein1024_888 extends Codec("skein1024-888", MULTIHASH, 0xb3cf, DRAFT)
  case skein1024_896 extends Codec("skein1024-896", MULTIHASH, 0xb3d0, DRAFT)
  case skein1024_904 extends Codec("skein1024-904", MULTIHASH, 0xb3d1, DRAFT)
  case skein1024_912 extends Codec("skein1024-912", MULTIHASH, 0xb3d2, DRAFT)
  case skein1024_920 extends Codec("skein1024-920", MULTIHASH, 0xb3d3, DRAFT)
  case skein1024_928 extends Codec("skein1024-928", MULTIHASH, 0xb3d4, DRAFT)
  case skein1024_936 extends Codec("skein1024-936", MULTIHASH, 0xb3d5, DRAFT)
  case skein1024_944 extends Codec("skein1024-944", MULTIHASH, 0xb3d6, DRAFT)
  case skein1024_952 extends Codec("skein1024-952", MULTIHASH, 0xb3d7, DRAFT)
  case skein1024_960 extends Codec("skein1024-960", MULTIHASH, 0xb3d8, DRAFT)
  case skein1024_968 extends Codec("skein1024-968", MULTIHASH, 0xb3d9, DRAFT)
  case skein1024_976 extends Codec("skein1024-976", MULTIHASH, 0xb3da, DRAFT)
  case skein1024_984 extends Codec("skein1024-984", MULTIHASH, 0xb3db, DRAFT)
  case skein1024_992 extends Codec("skein1024-992", MULTIHASH, 0xb3dc, DRAFT)
  case skein1024_1000 extends Codec("skein1024-1000", MULTIHASH, 0xb3dd, DRAFT)
  case skein1024_1008 extends Codec("skein1024-1008", MULTIHASH, 0xb3de, DRAFT)
  case skein1024_1016 extends Codec("skein1024-1016", MULTIHASH, 0xb3df, DRAFT)
  case skein1024_1024 extends Codec("skein1024-1024", MULTIHASH, 0xb3e0, DRAFT)

  /** Extremely fast non-cryptographic hash algorithm */
  case xxh_32 extends Codec("xxh-32", HASH, 0xb3e1, DRAFT)

  /** Extremely fast non-cryptographic hash algorithm */
  case xxh_64 extends Codec("xxh-64", HASH, 0xb3e2, DRAFT)

  /** Extremely fast non-cryptographic hash algorithm */
  case xxh3_64 extends Codec("xxh3-64", HASH, 0xb3e3, DRAFT)

  /** Extremely fast non-cryptographic hash algorithm */
  case xxh3_128 extends Codec("xxh3-128", HASH, 0xb3e4, DRAFT)

  /** Poseidon using BLS12-381 and arity of 2 with Filecoin parameters */
  case poseidon_bls12_381_a2_fc1 extends Codec("poseidon-bls12_381-a2-fc1", MULTIHASH, 0xb401, PERMANENT)

  /** Poseidon using BLS12-381 and arity of 2 with Filecoin parameters - high-security variant */
  case poseidon_bls12_381_a2_fc1_sc extends Codec("poseidon-bls12_381-a2-fc1-sc", MULTIHASH, 0xb402, DRAFT)

  /** The result of canonicalizing an input according to RDFC-1.0 and then expressing its hash value as a multihash
    * value.
    */
  case rdfc_1 extends Codec("rdfc-1", IPLD, 0xb403, DRAFT)

  /** SimpleSerialize (SSZ) serialization */
  case ssz extends Codec("ssz", SERIALIZATION, 0xb501, DRAFT)

  /** SSZ Merkle tree root using SHA2-256 as the hashing function and SSZ serialization for the block binary */
  case ssz_sha2_256_bmt extends Codec("ssz-sha2-256-bmt", MULTIHASH, 0xb502, DRAFT)

  /** Hash of concatenated SHA2-256 digests of 8*2^n MiB source chunks; n = ceil(log2(source_size/(10^4 * 8MiB))) */
  case sha2_256_chunked extends Codec("sha2-256-chunked", MULTIHASH, 0xb510, DRAFT)

  /** The result of canonicalizing an input according to JCS - JSON Canonicalisation Scheme (RFC 8785) */
  case json_jcs extends Codec("json-jcs", IPLD, 0xb601, DRAFT)

  /** ISCC (International Standard Content Code) - similarity preserving hash */
  case iscc extends Codec("iscc", SOFTHASH, 0xcc01, DRAFT)

  /** 0xcert Asset Imprint (root hash) */
  case zeroxcert_imprint_256 extends Codec("zeroxcert-imprint-256", ZEROCERT, 0xce11, DRAFT)

  /** Namespace for all not yet standard signature algorithms */
  case nonstandard_sig extends Codec("nonstandard-sig", VARSIG, 0xd000, DEPRECATED)

  /** Bcrypt-PBKDF key derivation function */
  case bcrypt_pbkdf extends Codec("bcrypt-pbkdf", MULTIHASH, 0xd00d, DRAFT)

  /** ES256K Siganture Algorithm (secp256k1) */
  case es256k extends Codec("es256k", VARSIG, 0xd0e7, DRAFT)

  /** G1 signature for BLS12-381 */
  case bls12_381_g1_sig extends Codec("bls12_381-g1-sig", VARSIG, 0xd0ea, DRAFT)

  /** G2 signature for BLS12-381 */
  case bls12_381_g2_sig extends Codec("bls12_381-g2-sig", VARSIG, 0xd0eb, DRAFT)

  /** Edwards-Curve Digital Signature Algorithm */
  case eddsa extends Codec("eddsa", VARSIG, 0xd0ed, DRAFT)

  /** EIP-191 Ethereum Signed Data Standard */
  case eip_191 extends Codec("eip-191", VARSIG, 0xd191, DRAFT)

  /** JSON object containing only the required members of a JWK (RFC 7518 and RFC 7517) representing the public key.
    * Serialisation based on JCS (RFC 8785)
    */
  case jwk_jcs_pub extends Codec("jwk_jcs-pub", KEY, 0xeb51, DRAFT)

  /** Filecoin piece or sector data commitment merkle node/root (CommP & CommD) */
  case fil_commitment_unsealed extends Codec("fil-commitment-unsealed", FILECOIN, 0xf101, PERMANENT)

  /** Filecoin sector data commitment merkle node/root - sealed and replicated (CommR) */
  case fil_commitment_sealed extends Codec("fil-commitment-sealed", FILECOIN, 0xf102, PERMANENT)
  case plaintextv2 extends Codec("plaintextv2", MULTIADDR, 0x706c61, DRAFT)

  /** Holochain v0 address    + 8 R-S (63 x Base-32) */
  case holochain_adr_v0 extends Codec("holochain-adr-v0", HOLOCHAIN, 0x807124, DRAFT)

  /** Holochain v1 address    + 8 R-S (63 x Base-32) */
  case holochain_adr_v1 extends Codec("holochain-adr-v1", HOLOCHAIN, 0x817124, DRAFT)

  /** Holochain v0 public key + 8 R-S (63 x Base-32) */
  case holochain_key_v0 extends Codec("holochain-key-v0", HOLOCHAIN, 0x947124, DRAFT)

  /** Holochain v1 public key + 8 R-S (63 x Base-32) */
  case holochain_key_v1 extends Codec("holochain-key-v1", HOLOCHAIN, 0x957124, DRAFT)

  /** Holochain v0 signature  + 8 R-S (63 x Base-32) */
  case holochain_sig_v0 extends Codec("holochain-sig-v0", HOLOCHAIN, 0xa27124, DRAFT)

  /** Holochain v1 signature  + 8 R-S (63 x Base-32) */
  case holochain_sig_v1 extends Codec("holochain-sig-v1", HOLOCHAIN, 0xa37124, DRAFT)

  /** Skynet Namespace */
  case skynet_ns extends Codec("skynet-ns", NAMESPACE, 0xb19910, DRAFT)

  /** Arweave Namespace */
  case arweave_ns extends Codec("arweave-ns", NAMESPACE, 0xb29910, DRAFT)

  /** Subspace Network Namespace */
  case subspace_ns extends Codec("subspace-ns", NAMESPACE, 0xb39910, DRAFT)

  /** Kumandra Network Namespace */
  case kumandra_ns extends Codec("kumandra-ns", NAMESPACE, 0xb49910, DRAFT)

  /** ES256 Signature Algorithm */
  case es256 extends Codec("es256", VARSIG, 0xd01200, DRAFT)

  /** ES384 Signature Algorithm */
  case es284 extends Codec("es284", VARSIG, 0xd01201, DRAFT)

  /** ES512 Signature Algorithm */
  case es512 extends Codec("es512", VARSIG, 0xd01202, DRAFT)

  /** RS256 Signature Algorithm */
  case rs256 extends Codec("rs256", VARSIG, 0xd01205, DRAFT)

  /** ES256K (secp256k1) Signature as Multisig */
  case es256k_msig extends Codec("es256k-msig", MULTISIG, 0xd01300, DRAFT)

  /** G1 signature for BLS-12381-G2 as Multisig */
  case bls12_381_g1_msig extends Codec("bls12_381-g1-msig", MULTISIG, 0xd01301, DRAFT)

  /** G2 signature for BLS-12381-G1 as Multisig */
  case bls12_381_g2_msig extends Codec("bls12_381-g2-msig", MULTISIG, 0xd01302, DRAFT)

  /** Edwards-Curve Digital Signature as Multisig */
  case eddsa_msig extends Codec("eddsa-msig", MULTISIG, 0xd01303, DRAFT)

  /** G1 threshold signature share for BLS-12381-G2 as Multisig */
  case bls12_381_g1_share_msig extends Codec("bls12_381-g1-share-msig", MULTISIG, 0xd01304, DRAFT)

  /** G2 threshold signature share for BLS-12381-G1 as Multisig */
  case bls12_381_g2_share_msig extends Codec("bls12_381-g2-share-msig", MULTISIG, 0xd01305, DRAFT)

  /** Lamport signature as Multisig */
  case lamport_msig extends Codec("lamport-msig", MULTISIG, 0xd01306, DRAFT)

  /** Lamport threshold signature share as Multisig */
  case lamport_share_msig extends Codec("lamport-share-msig", MULTISIG, 0xd01307, DRAFT)

  /** ECDSA P-256 Signature as Multisig */
  case es256_msig extends Codec("es256-msig", MULTISIG, 0xd01308, DRAFT)

  /** ECDSA P-384 Signature as Multisig */
  case es384_msig extends Codec("es384-msig", MULTISIG, 0xd01309, DRAFT)

  /** ECDSA P-521 Signature as Multisig */
  case es521_msig extends Codec("es521-msig", MULTISIG, 0xd0130a, DRAFT)

  /** RS256 Signature as Multisig */
  case rs256_msig extends Codec("rs256-msig", MULTISIG, 0xd0130b, DRAFT)

  /** SCION Internet architecture */
  case scion extends Codec("scion", MULTIADDR, 0xd02000, DRAFT)

}
