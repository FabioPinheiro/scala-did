package fmgp.multiformats

enum MulticodecStatus {
  case PERMANENT, DRAFT, DEPRECATED
}

import MulticodecStatus.*

/** https://github.com/multiformats/multicodec/blob/352d05ad430713088e867216152725f581387bc8/table.csv */
enum Multicodec(name: String, tag: String, code: Array[Byte], status: MulticodecStatus) {

  // raw binary
  case identity extends Multicodec("identity", "multihash", Array[Byte](0x00), PERMANENT)
  // CIDv1
  case cidv1 extends Multicodec("cidv1", "cid", Array[Byte](0x01), PERMANENT)
  // CIDv2
  case cidv2 extends Multicodec("cidv2", "cid", Array[Byte](0x02), DRAFT)
  // CIDv3
  case cidv3 extends Multicodec("cidv3", "cid", Array[Byte](0x03), DRAFT)
  case ip4 extends Multicodec("ip4", "multiaddr", Array[Byte](0x04), PERMANENT)
  case tcp extends Multicodec("tcp", "multiaddr", Array[Byte](0x06), PERMANENT)
  case sha1 extends Multicodec("sha1", "multihash", Array[Byte](0x11), PERMANENT)
  case sha2_256 extends Multicodec("sha2-256", "multihash", Array[Byte](0x12), PERMANENT)
  case sha2_512 extends Multicodec("sha2-512", "multihash", Array[Byte](0x13), PERMANENT)
  case sha3_512 extends Multicodec("sha3-512", "multihash", Array[Byte](0x14), PERMANENT)
  case sha3_384 extends Multicodec("sha3-384", "multihash", Array[Byte](0x15), PERMANENT)
  case sha3_256 extends Multicodec("sha3-256", "multihash", Array[Byte](0x16), PERMANENT)
  case sha3_224 extends Multicodec("sha3-224", "multihash", Array[Byte](0x17), PERMANENT)
  case shake_128 extends Multicodec("shake-128", "multihash", Array[Byte](0x18), DRAFT)
  case shake_256 extends Multicodec("shake-256", "multihash", Array[Byte](0x19), DRAFT)
  // keccak has variable output length. The number specifies the core length
  case keccak_224 extends Multicodec("keccak-224", "multihash", Array[Byte](0x1a), DRAFT)
  case keccak_256 extends Multicodec("keccak-256", "multihash", Array[Byte](0x1b), DRAFT)
  case keccak_384 extends Multicodec("keccak-384", "multihash", Array[Byte](0x1c), DRAFT)
  case keccak_512 extends Multicodec("keccak-512", "multihash", Array[Byte](0x1d), DRAFT)
  // BLAKE3 has a default 32 byte output length. The maximum length is (2^64)-1 bytes
  case blake3 extends Multicodec("blake3", "multihash", Array[Byte](0x1e), DRAFT)
  // aka SHA-384; as specified by FIPS 180-4
  case sha2_384 extends Multicodec("sha2-384", "multihash", Array[Byte](0x20), PERMANENT)
  case dccp extends Multicodec("dccp", "multiaddr", Array[Byte](0x21), DRAFT)
  // The first 64-bits of a murmur3-x64-128 - used for UnixFS directory sharding
  case murmur3_x64_64 extends Multicodec("murmur3-x64-64", "hash", Array[Byte](0x22), PERMANENT)
  case murmur3_32 extends Multicodec("murmur3-32", "hash", Array[Byte](0x23), DRAFT)
  case ip6 extends Multicodec("ip6", "multiaddr", Array[Byte](0x29), PERMANENT)
  case ip6zone extends Multicodec("ip6zone", "multiaddr", Array[Byte](0x2a), DRAFT)
  // CIDR mask for IP addresses
  case ipcidr extends Multicodec("ipcidr", "multiaddr", Array[Byte](0x2b), DRAFT)
  // Namespace for string paths. Corresponds to `/` in ASCII
  case path extends Multicodec("path", "namespace", Array[Byte](0x2f), PERMANENT)
  case multicodec extends Multicodec("multicodec", "multiformat", Array[Byte](0x30), DRAFT)
  case multihash extends Multicodec("multihash", "multiformat", Array[Byte](0x31), DRAFT)
  case multiaddr extends Multicodec("multiaddr", "multiformat", Array[Byte](0x32), DRAFT)
  case multibase extends Multicodec("multibase", "multiformat", Array[Byte](0x33), DRAFT)
  // Variable signature (varsig) multiformat
  case varsig extends Multicodec("varsig", "multiformat", Array[Byte](0x34), DRAFT)
  case dns extends Multicodec("dns", "multiaddr", Array[Byte](0x35), PERMANENT)
  case dns4 extends Multicodec("dns4", "multiaddr", Array[Byte](0x36), PERMANENT)
  case dns6 extends Multicodec("dns6", "multiaddr", Array[Byte](0x37), PERMANENT)
  case dnsaddr extends Multicodec("dnsaddr", "multiaddr", Array[Byte](0x38), PERMANENT)
  // Protocol Buffers
  case protobuf extends Multicodec("protobuf", "serialization", Array[Byte](0x50), DRAFT)
  // CBOR
  case cbor extends Multicodec("cbor", "ipld", Array[Byte](0x51), PERMANENT)
  // raw binary
  case raw extends Multicodec("raw", "ipld", Array[Byte](0x55), PERMANENT)
  case dbl_sha2_256 extends Multicodec("dbl-sha2-256", "multihash", Array[Byte](0x56), DRAFT)
  // recursive length prefix
  case rlp extends Multicodec("rlp", "serialization", Array[Byte](0x60), DRAFT)
  // bencode
  case bencode extends Multicodec("bencode", "serialization", Array[Byte](0x63), DRAFT)
  // MerkleDAG protobuf
  case dag_pb extends Multicodec("dag-pb", "ipld", Array[Byte](0x70), PERMANENT)
  // MerkleDAG cbor
  case dag_cbor extends Multicodec("dag-cbor", "ipld", Array[Byte](0x71), PERMANENT)
  // Libp2p Public Key
  case libp2p_key extends Multicodec("libp2p-key", "ipld", Array[Byte](0x72), PERMANENT)
  // Raw Git object
  case git_raw extends Multicodec("git-raw", "ipld", Array[Byte](0x78), PERMANENT)
  // Torrent file info field (bencoded)
  case torrent_info extends Multicodec("torrent-info", "ipld", Array[Byte](0x7b), DRAFT)
  // Torrent file (bencoded)
  case torrent_file extends Multicodec("torrent-file", "ipld", Array[Byte](0x7c), DRAFT)
  // BLAKE3 hash sequence - per Iroh collections spec
  case blake3_hashseq extends Multicodec("blake3-hashseq", "ipld", Array[Byte](0x80.toByte), DRAFT)
  // Leofcoin Block
  case leofcoin_block extends Multicodec("leofcoin-block", "ipld", Array[Byte](0x81.toByte), DRAFT)
  // Leofcoin Transaction
  case leofcoin_tx extends Multicodec("leofcoin-tx", "ipld", Array[Byte](0x82.toByte), DRAFT)
  // Leofcoin Peer Reputation
  case leofcoin_pr extends Multicodec("leofcoin-pr", "ipld", Array[Byte](0x83.toByte), DRAFT)
  case sctp extends Multicodec("sctp", "multiaddr", Array[Byte](0x84.toByte), DRAFT)
  // MerkleDAG JOSE
  case dag_jose extends Multicodec("dag-jose", "ipld", Array[Byte](0x85.toByte), DRAFT)
  // MerkleDAG COSE
  case dag_cose extends Multicodec("dag-cose", "ipld", Array[Byte](0x86.toByte), DRAFT)
  // LBRY Address
  case lbry extends Multicodec("lbry", "namespace", Array[Byte](0x8c.toByte), DRAFT)
  // Ethereum Header (RLP)
  case eth_block extends Multicodec("eth-block", "ipld", Array[Byte](0x90.toByte), PERMANENT)
  // Ethereum Header List (RLP)
  case eth_block_list extends Multicodec("eth-block-list", "ipld", Array[Byte](0x91.toByte), PERMANENT)
  //  Ethereum Transaction Trie (Eth-Trie)
  case eth_tx_trie extends Multicodec("eth-tx-trie", "ipld", Array[Byte](0x92.toByte), PERMANENT)
  // Ethereum Transaction (MarshalBinary)
  case eth_tx extends Multicodec("eth-tx", "ipld", Array[Byte](0x93.toByte), PERMANENT)
  //  Ethereum Transaction Receipt Trie (Eth-Trie)
  case eth_tx_receipt_trie extends Multicodec("eth-tx-receipt-trie", "ipld", Array[Byte](0x94.toByte), PERMANENT)
  // Ethereum Transaction Receipt (MarshalBinary)
  case eth_tx_receipt extends Multicodec("eth-tx-receipt", "ipld", Array[Byte](0x95.toByte), PERMANENT)
  // Ethereum State Trie (Eth-Secure-Trie)
  case eth_state_trie extends Multicodec("eth-state-trie", "ipld", Array[Byte](0x96.toByte), PERMANENT)
  // Ethereum Account Snapshot (RLP)
  case eth_account_snapshot extends Multicodec("eth-account-snapshot", "ipld", Array[Byte](0x97.toByte), PERMANENT)
  //  Ethereum Contract Storage Trie (Eth-Secure-Trie)
  case eth_storage_trie extends Multicodec("eth-storage-trie", "ipld", Array[Byte](0x98.toByte), PERMANENT)
  // Ethereum Transaction Receipt Log Trie (Eth-Trie)
  case eth_receipt_log_trie extends Multicodec("eth-receipt-log-trie", "ipld", Array[Byte](0x99.toByte), DRAFT)
  // Ethereum Transaction Receipt Log (RLP)
  case eth_receipt_log extends Multicodec("eth-receipt-log", "ipld", Array[Byte](0x9a.toByte), DRAFT)
  // 128-bit AES symmetric key
  case aes_128 extends Multicodec("aes-128", "key", Array[Byte](0xa0.toByte), DRAFT)
  // 192-bit AES symmetric key
  case aes_192 extends Multicodec("aes-192", "key", Array[Byte](0xa1.toByte), DRAFT)
  // 256-bit AES symmetric key
  case aes_256 extends Multicodec("aes-256", "key", Array[Byte](0xa2.toByte), DRAFT)
  // 128-bit ChaCha symmetric key
  case chacha_128 extends Multicodec("chacha-128", "key", Array[Byte](0xa3.toByte), DRAFT)
  // 256-bit ChaCha symmetric key
  case chacha_256 extends Multicodec("chacha-256", "key", Array[Byte](0xa4.toByte), DRAFT)
  // Bitcoin Block
  case bitcoin_block extends Multicodec("bitcoin-block", "ipld", Array[Byte](0xb0.toByte), PERMANENT)
  // Bitcoin Tx
  case bitcoin_tx extends Multicodec("bitcoin-tx", "ipld", Array[Byte](0xb1.toByte), PERMANENT)
  // Bitcoin Witness Commitment
  case bitcoin_witness_commitment
      extends Multicodec("bitcoin-witness-commitment", "ipld", Array[Byte](0xb2.toByte), PERMANENT)
  // Zcash Block
  case zcash_block extends Multicodec("zcash-block", "ipld", Array[Byte](0xc0.toByte), PERMANENT)
  // Zcash Tx
  case zcash_tx extends Multicodec("zcash-tx", "ipld", Array[Byte](0xc1.toByte), PERMANENT)
  // CAIP-50 multi-chain account id
  case caip_50 extends Multicodec("caip-50", "multiformat", Array[Byte](0xca.toByte), DRAFT)
  // Ceramic Stream Id
  case streamid extends Multicodec("streamid", "namespace", Array[Byte](0xce.toByte), DRAFT)
  // Stellar Block
  case stellar_block extends Multicodec("stellar-block", "ipld", Array[Byte](0xd0.toByte), DRAFT)
  // Stellar Tx
  case stellar_tx extends Multicodec("stellar-tx", "ipld", Array[Byte](0xd1.toByte), DRAFT)
  case md4 extends Multicodec("md4", "multihash", Array[Byte](0xd4.toByte), DRAFT)
  case md5 extends Multicodec("md5", "multihash", Array[Byte](0xd5.toByte), DRAFT)
  // Decred Block
  case decred_block extends Multicodec("decred-block", "ipld", Array[Byte](0xe0.toByte), DRAFT)
  // Decred Tx
  case decred_tx extends Multicodec("decred-tx", "ipld", Array[Byte](0xe1.toByte), DRAFT)
  // IPLD path
  case ipld extends Multicodec("ipld", "namespace", Array[Byte](0xe2.toByte), DRAFT)
  // IPFS path
  case ipfs extends Multicodec("ipfs", "namespace", Array[Byte](0xe3.toByte), DRAFT)
  // Swarm path
  case swarm extends Multicodec("swarm", "namespace", Array[Byte](0xe4.toByte), DRAFT)
  // IPNS path
  case ipns extends Multicodec("ipns", "namespace", Array[Byte](0xe5.toByte), DRAFT)
  // ZeroNet site address
  case zeronet extends Multicodec("zeronet", "namespace", Array[Byte](0xe6.toByte), DRAFT)
  // Secp256k1 public key (compressed)
  case secp256k1_pub extends Multicodec("secp256k1-pub", "key", Array[Byte](0xe7.toByte), DRAFT)
  // DNSLink path
  case dnslink extends Multicodec("dnslink", "namespace", Array[Byte](0xe8.toByte), PERMANENT)
  // BLS12-381 public key in the G1 field
  case bls12_381_g1_pub extends Multicodec("bls12_381-g1-pub", "key", Array[Byte](0xea.toByte), DRAFT)
  // BLS12-381 public key in the G2 field
  case bls12_381_g2_pub extends Multicodec("bls12_381-g2-pub", "key", Array[Byte](0xeb.toByte), DRAFT)
  // Curve25519 public key
  case x25519_pub extends Multicodec("x25519-pub", "key", Array[Byte](0xec.toByte), DRAFT)
  // Ed25519 public key
  case ed25519_pub extends Multicodec("ed25519-pub", "key", Array[Byte](0xed.toByte), DRAFT)
  // BLS12-381 concatenated public keys in both the G1 and G2 fields
  case bls12_381_g1g2_pub extends Multicodec("bls12_381-g1g2-pub", "key", Array[Byte](0xee.toByte), DRAFT)
  // Sr25519 public key
  case sr25519_pub extends Multicodec("sr25519-pub", "key", Array[Byte](0xef.toByte), DRAFT)
  // Dash Block
  case dash_block extends Multicodec("dash-block", "ipld", Array[Byte](0xf0.toByte), DRAFT)
  // Dash Tx
  case dash_tx extends Multicodec("dash-tx", "ipld", Array[Byte](0xf1.toByte), DRAFT)
  // Swarm Manifest
  case swarm_manifest extends Multicodec("swarm-manifest", "ipld", Array[Byte](0xfa.toByte), DRAFT)
  // Swarm Feed
  case swarm_feed extends Multicodec("swarm-feed", "ipld", Array[Byte](0xfb.toByte), DRAFT)
  // Swarm BeeSon
  case beeson extends Multicodec("beeson", "ipld", Array[Byte](0xfc.toByte), DRAFT)
  case udp extends Multicodec("udp", "multiaddr", Array[Byte](0x01.toByte, 0x11.toByte), DRAFT)
  // Use webrtc or webrtc-direct instead
  case p2p_webrtc_star
      extends Multicodec("p2p-webrtc-star", "multiaddr", Array[Byte](0x01.toByte, 0x13.toByte), DEPRECATED)
  // Use webrtc or webrtc-direct instead
  case p2p_webrtc_direct
      extends Multicodec("p2p-webrtc-direct", "multiaddr", Array[Byte](0x01.toByte, 0x14.toByte), DEPRECATED)
  case p2p_stardust extends Multicodec("p2p-stardust", "multiaddr", Array[Byte](0x01.toByte, 0x15.toByte), DEPRECATED)
  // ICE-lite webrtc transport with SDP munging during connection establishment and without use of a STUN server
  case webrtc_direct extends Multicodec("webrtc-direct", "multiaddr", Array[Byte](0x01.toByte, 0x18.toByte), DRAFT)
  // webrtc transport where connection establishment is according to w3c spec
  case webrtc extends Multicodec("webrtc", "multiaddr", Array[Byte](0x01.toByte, 0x19.toByte), DRAFT)
  case p2p_circuit extends Multicodec("p2p-circuit", "multiaddr", Array[Byte](0x01.toByte, 0x22.toByte), PERMANENT)
  // MerkleDAG json
  case dag_json extends Multicodec("dag-json", "ipld", Array[Byte](0x01.toByte, 0x29.toByte), PERMANENT)
  case udt extends Multicodec("udt", "multiaddr", Array[Byte](0x01.toByte, 0x2d.toByte), DRAFT)
  case utp extends Multicodec("utp", "multiaddr", Array[Byte](0x01.toByte, 0x2e.toByte), DRAFT)
  // CRC-32 non-cryptographic hash algorithm (IEEE 802.3)
  case crc32 extends Multicodec("crc32", "hash", Array[Byte](0x01.toByte, 0x32.toByte), DRAFT)
  // CRC-64 non-cryptographic hash algorithm (ECMA-182 - Annex B)
  case crc64_ecma extends Multicodec("crc64-ecma", "hash", Array[Byte](0x01.toByte, 0x64.toByte), DRAFT)
  case unix extends Multicodec("unix", "multiaddr", Array[Byte](0x01.toByte, 0x90.toByte), PERMANENT)
  // Textile Thread
  case thread extends Multicodec("thread", "multiaddr", Array[Byte](0x01.toByte, 0x96.toByte), DRAFT)
  // libp2p
  case p2p extends Multicodec("p2p", "multiaddr", Array[Byte](0x01.toByte, 0xa5.toByte), PERMANENT)
  case https extends Multicodec("https", "multiaddr", Array[Byte](0x01.toByte, 0xbb.toByte), DRAFT)
  case onion extends Multicodec("onion", "multiaddr", Array[Byte](0x01.toByte, 0xbc.toByte), DRAFT)
  case onion3 extends Multicodec("onion3", "multiaddr", Array[Byte](0x01.toByte, 0xbd.toByte), DRAFT)
  // I2P base64 (raw public key)
  case garlic64 extends Multicodec("garlic64", "multiaddr", Array[Byte](0x01.toByte, 0xbe.toByte), DRAFT)
  // I2P base32 (hashed public key or encoded public key/checksum+optional secret)
  case garlic32 extends Multicodec("garlic32", "multiaddr", Array[Byte](0x01.toByte, 0xbf.toByte), DRAFT)
  case tls extends Multicodec("tls", "multiaddr", Array[Byte](0x01.toByte, 0xc0.toByte), DRAFT)
  // Server Name Indication RFC 6066 § 3
  case sni extends Multicodec("sni", "multiaddr", Array[Byte](0x01.toByte, 0xc1.toByte), DRAFT)
  case noise extends Multicodec("noise", "multiaddr", Array[Byte](0x01.toByte, 0xc6.toByte), DRAFT)
  // Secure Scuttlebutt - Secret Handshake Stream
  case shs extends Multicodec("shs", "multiaddr", Array[Byte](0x01.toByte, 0xc8.toByte), DRAFT)
  case quic extends Multicodec("quic", "multiaddr", Array[Byte](0x01.toByte, 0xcc.toByte), PERMANENT)
  case quic_v1 extends Multicodec("quic-v1", "multiaddr", Array[Byte](0x01.toByte, 0xcd.toByte), PERMANENT)
  case webtransport extends Multicodec("webtransport", "multiaddr", Array[Byte](0x01.toByte, 0xd1.toByte), DRAFT)
  // TLS certificate's fingerprint as a multihash
  case certhash extends Multicodec("certhash", "multiaddr", Array[Byte](0x01.toByte, 0xd2.toByte), DRAFT)
  case ws extends Multicodec("ws", "multiaddr", Array[Byte](0x01.toByte, 0xdd.toByte), PERMANENT)
  case wss extends Multicodec("wss", "multiaddr", Array[Byte](0x01.toByte, 0xde.toByte), PERMANENT)
  case p2p_websocket_star
      extends Multicodec("p2p-websocket-star", "multiaddr", Array[Byte](0x01.toByte, 0xdf.toByte), PERMANENT)
  case http extends Multicodec("http", "multiaddr", Array[Byte](0x01.toByte, 0xe0.toByte), DRAFT)
  // Percent-encoded path to an HTTP resource
  case http_path extends Multicodec("http-path", "multiaddr", Array[Byte](0x01.toByte, 0xe1.toByte), DRAFT)
  // SoftWare Heritage persistent IDentifier version 1 snapshot
  case swhid_1_snp extends Multicodec("swhid-1-snp", "ipld", Array[Byte](0x01.toByte, 0xf0.toByte), DRAFT)
  // JSON (UTF-8-encoded)
  case json extends Multicodec("json", "ipld", Array[Byte](0x02.toByte, 0x00.toByte), PERMANENT)
  // MessagePack
  case messagepack extends Multicodec("messagepack", "serialization", Array[Byte](0x02.toByte, 0x01.toByte), DRAFT)
  // Content Addressable aRchive (CAR)
  case car extends Multicodec("car", "serialization", Array[Byte](0x02.toByte, 0x02.toByte), DRAFT)
  // Signed IPNS Record
  case ipns_record extends Multicodec("ipns-record", "serialization", Array[Byte](0x03.toByte, 0x00.toByte), PERMANENT)
  // libp2p peer record type
  case libp2p_peer_record
      extends Multicodec("libp2p-peer-record", "libp2p", Array[Byte](0x03.toByte, 0x01.toByte), PERMANENT)
  // libp2p relay reservation voucher
  case libp2p_relay_rsvp
      extends Multicodec("libp2p-relay-rsvp", "libp2p", Array[Byte](0x03.toByte, 0x02.toByte), PERMANENT)
  // in memory transport for self-dialing and testing; arbitrary
  case memorytransport extends Multicodec("memorytransport", "libp2p", Array[Byte](0x03.toByte, 0x09.toByte), PERMANENT)
  // CARv2 IndexSorted index format
  case car_index_sorted
      extends Multicodec("car-index-sorted", "serialization", Array[Byte](0x04.toByte, 0x00.toByte), DRAFT)
  // CARv2 MultihashIndexSorted index format
  case car_multihash_index_sorted
      extends Multicodec("car-multihash-index-sorted", "serialization", Array[Byte](0x04.toByte, 0x01.toByte), DRAFT)
  // Bitswap datatransfer
  case transport_bitswap
      extends Multicodec("transport-bitswap", "transport", Array[Byte](0x09.toByte, 0x00.toByte), DRAFT)
  // Filecoin graphsync datatransfer
  case transport_graphsync_filecoinv1
      extends Multicodec("transport-graphsync-filecoinv1", "transport", Array[Byte](0x09.toByte, 0x10.toByte), DRAFT)
  // HTTP IPFS Gateway trustless datatransfer
  case transport_ipfs_gateway_http
      extends Multicodec("transport-ipfs-gateway-http", "transport", Array[Byte](0x09.toByte, 0x20.toByte), DRAFT)
  // Compact encoding for Decentralized Identifers
  case multidid extends Multicodec("multidid", "multiformat", Array[Byte](0x0d.toByte, 0x1d.toByte), DRAFT)
  // SHA2-256 with the two most significant bits from the last byte zeroed (as via a mask with 0b00111111) - used for proving trees as in Filecoin
  case sha2_256_trunc254_padded
      extends Multicodec("sha2-256-trunc254-padded", "multihash", Array[Byte](0x10.toByte, 0x12.toByte), PERMANENT)
  // aka SHA-224; as specified by FIPS 180-4.
  case sha2_224 extends Multicodec("sha2-224", "multihash", Array[Byte](0x10.toByte, 0x13.toByte), PERMANENT)
  // aka SHA-512/224; as specified by FIPS 180-4.
  case sha2_512_224 extends Multicodec("sha2-512-224", "multihash", Array[Byte](0x10.toByte, 0x14.toByte), PERMANENT)
  // aka SHA-512/256; as specified by FIPS 180-4.
  case sha2_512_256 extends Multicodec("sha2-512-256", "multihash", Array[Byte](0x10.toByte, 0x15.toByte), PERMANENT)
  case murmur3_x64_128 extends Multicodec("murmur3-x64-128", "hash", Array[Byte](0x10.toByte, 0x22.toByte), DRAFT)
  case ripemd_128 extends Multicodec("ripemd-128", "multihash", Array[Byte](0x10.toByte, 0x52.toByte), DRAFT)
  case ripemd_160 extends Multicodec("ripemd-160", "multihash", Array[Byte](0x10.toByte, 0x53.toByte), DRAFT)
  case ripemd_256 extends Multicodec("ripemd-256", "multihash", Array[Byte](0x10.toByte, 0x54.toByte), DRAFT)
  case ripemd_320 extends Multicodec("ripemd-320", "multihash", Array[Byte](0x10.toByte, 0x55.toByte), DRAFT)
  case x11 extends Multicodec("x11", "multihash", Array[Byte](0x11.toByte, 0x00.toByte), DRAFT)
  // P-256 public Key (compressed)
  case p256_pub extends Multicodec("p256-pub", "key", Array[Byte](0x12.toByte, 0x00.toByte), DRAFT)
  // P-384 public Key (compressed)
  case p384_pub extends Multicodec("p384-pub", "key", Array[Byte](0x12.toByte, 0x01.toByte), DRAFT)
  // P-521 public Key (compressed)
  case p521_pub extends Multicodec("p521-pub", "key", Array[Byte](0x12.toByte, 0x02.toByte), DRAFT)
  // Ed448 public Key
  case ed448_pub extends Multicodec("ed448-pub", "key", Array[Byte](0x12.toByte, 0x03.toByte), DRAFT)
  // X448 public Key
  case x448_pub extends Multicodec("x448-pub", "key", Array[Byte](0x12.toByte, 0x04.toByte), DRAFT)
  // RSA public key. DER-encoded ASN.1 type RSAPublicKey according to IETF RFC 8017 (PKCS #1)
  case rsa_pub extends Multicodec("rsa-pub", "key", Array[Byte](0x12.toByte, 0x05.toByte), DRAFT)
  // SM2 public key (compressed)
  case sm2_pub extends Multicodec("sm2-pub", "key", Array[Byte](0x12.toByte, 0x06.toByte), DRAFT)
  // Verifiable Long-lived ADdress
  case vlad extends Multicodec("vlad", "vlad", Array[Byte](0x12.toByte, 0x07.toByte), DRAFT)
  // Verifiable and permissioned append-only log
  case provenance_log
      extends Multicodec("provenance-log", "serialization", Array[Byte](0x12.toByte, 0x08.toByte), DRAFT)
  // Verifiable and permissioned append-only log entry
  case provenance_log_entry
      extends Multicodec("provenance-log-entry", "serialization", Array[Byte](0x12.toByte, 0x09.toByte), DRAFT)
  // Verifiable and permissioned append-only log script
  case provenance_log_script
      extends Multicodec("provenance-log-script", "serialization", Array[Byte](0x12.toByte, 0x0a.toByte), DRAFT)
  // ML-KEM 512 public key; as specified by FIPS 203
  case mlkem_512_pub extends Multicodec("mlkem-512-pub", "key", Array[Byte](0x12.toByte, 0x0b.toByte), DRAFT)
  // ML-KEM 768 public key; as specified by FIPS 203
  case mlkem_768_pub extends Multicodec("mlkem-768-pub", "key", Array[Byte](0x12.toByte, 0x0c.toByte), DRAFT)
  // ML-KEM 1024 public key; as specified by FIPS 203
  case mlkem_1024_pub extends Multicodec("mlkem-1024-pub", "key", Array[Byte](0x12.toByte, 0x0d.toByte), DRAFT)
  // Digital signature multiformat
  case multisig extends Multicodec("multisig", "multiformat", Array[Byte](0x12.toByte, 0x39.toByte), DRAFT)
  // Encryption key multiformat
  case multikey extends Multicodec("multikey", "multiformat", Array[Byte](0x12.toByte, 0x3a.toByte), DRAFT)
  // Nonce random value
  case nonce extends Multicodec("nonce", "nonce", Array[Byte](0x12.toByte, 0x3b.toByte), DRAFT)
  // Ed25519 private key
  case ed25519_priv extends Multicodec("ed25519-priv", "key", Array[Byte](0x13.toByte, 0x00.toByte), DRAFT)
  // Secp256k1 private key
  case secp256k1_priv extends Multicodec("secp256k1-priv", "key", Array[Byte](0x13.toByte, 0x01.toByte), DRAFT)
  // Curve25519 private key
  case x25519_priv extends Multicodec("x25519-priv", "key", Array[Byte](0x13.toByte, 0x02.toByte), DRAFT)
  // Sr25519 private key
  case sr25519_priv extends Multicodec("sr25519-priv", "key", Array[Byte](0x13.toByte, 0x03.toByte), DRAFT)
  // RSA private key
  case rsa_priv extends Multicodec("rsa-priv", "key", Array[Byte](0x13.toByte, 0x05.toByte), DRAFT)
  // P-256 private key
  case p256_priv extends Multicodec("p256-priv", "key", Array[Byte](0x13.toByte, 0x06.toByte), DRAFT)
  // P-384 private key
  case p384_priv extends Multicodec("p384-priv", "key", Array[Byte](0x13.toByte, 0x07.toByte), DRAFT)
  // P-521 private key
  case p521_priv extends Multicodec("p521-priv", "key", Array[Byte](0x13.toByte, 0x08.toByte), DRAFT)
  // BLS12-381 G1 private key
  case bls12_381_g1_priv extends Multicodec("bls12_381-g1-priv", "key", Array[Byte](0x13.toByte, 0x09.toByte), DRAFT)
  // BLS12-381 G2 private key
  case bls12_381_g2_priv extends Multicodec("bls12_381-g2-priv", "key", Array[Byte](0x13.toByte, 0x0a.toByte), DRAFT)
  // BLS12-381 G1 and G2 private key
  case bls12_381_g1g2_priv
      extends Multicodec("bls12_381-g1g2-priv", "key", Array[Byte](0x13.toByte, 0x0b.toByte), DRAFT)
  // BLS12-381 G1 public key share
  case bls12_381_g1_pub_share
      extends Multicodec("bls12_381-g1-pub-share", "key", Array[Byte](0x13.toByte, 0x0c.toByte), DRAFT)
  // BLS12-381 G2 public key share
  case bls12_381_g2_pub_share
      extends Multicodec("bls12_381-g2-pub-share", "key", Array[Byte](0x13.toByte, 0x0d.toByte), DRAFT)
  // BLS12-381 G1 private key share
  case bls12_381_g1_priv_share
      extends Multicodec("bls12_381-g1-priv-share", "key", Array[Byte](0x13.toByte, 0x0e.toByte), DRAFT)
  // BLS12-381 G2 private key share
  case bls12_381_g2_priv_share
      extends Multicodec("bls12_381-g2-priv-share", "key", Array[Byte](0x13.toByte, 0x0f.toByte), DRAFT)
  // Lamport public key based on SHA3-512
  case lamport_sha3_512_pub
      extends Multicodec("lamport-sha3-512-pub", "key", Array[Byte](0x1a.toByte, 0x14.toByte), DRAFT)
  // Lamport public key based on SHA3-384
  case lamport_sha3_384_pub
      extends Multicodec("lamport-sha3-384-pub", "key", Array[Byte](0x1a.toByte, 0x15.toByte), DRAFT)
  // Lamport public key based on SHA3-256
  case lamport_sha3_256_pub
      extends Multicodec("lamport-sha3-256-pub", "key", Array[Byte](0x1a.toByte, 0x16.toByte), DRAFT)
  // Lamport private key based on SHA3-512
  case lamport_sha3_512_priv
      extends Multicodec("lamport-sha3-512-priv", "key", Array[Byte](0x1a.toByte, 0x24.toByte), DRAFT)
  // Lamport private key based on SHA3-384
  case lamport_sha3_384_priv
      extends Multicodec("lamport-sha3-384-priv", "key", Array[Byte](0x1a.toByte, 0x25.toByte), DRAFT)
  // Lamport private key based on SHA3-256
  case lamport_sha3_256_priv
      extends Multicodec("lamport-sha3-256-priv", "key", Array[Byte](0x1a.toByte, 0x26.toByte), DRAFT)
  // Lamport private key share based on SHA3-512 and split with Shamir gf256
  case lamport_sha3_512_priv_share
      extends Multicodec("lamport-sha3-512-priv-share", "key", Array[Byte](0x1a.toByte, 0x34.toByte), DRAFT)
  // Lamport private key share based on SHA3-384 and split with Shamir gf256
  case lamport_sha3_384_priv_share
      extends Multicodec("lamport-sha3-384-priv-share", "key", Array[Byte](0x1a.toByte, 0x35.toByte), DRAFT)
  // Lamport private key share based on SHA3-256 and split with Shamir gf256
  case lamport_sha3_256_priv_share
      extends Multicodec("lamport-sha3-256-priv-share", "key", Array[Byte](0x1a.toByte, 0x36.toByte), DRAFT)
  // Lamport signature based on SHA3-512
  case lamport_sha3_512_sig
      extends Multicodec("lamport-sha3-512-sig", "multisig", Array[Byte](0x1a.toByte, 0x44.toByte), DRAFT)
  // Lamport signature based on SHA3-384
  case lamport_sha3_384_sig
      extends Multicodec("lamport-sha3-384-sig", "multisig", Array[Byte](0x1a.toByte, 0x45.toByte), DRAFT)
  // Lamport signature based on SHA3-256
  case lamport_sha3_256_sig
      extends Multicodec("lamport-sha3-256-sig", "multisig", Array[Byte](0x1a.toByte, 0x46.toByte), DRAFT)
  // Lamport signature share based on SHA3-512 and split with Shamir gf256
  case lamport_sha3_512_sig_share
      extends Multicodec("lamport-sha3-512-sig-share", "multisig", Array[Byte](0x1a.toByte, 0x54.toByte), DRAFT)
  // Lamport signature share based on SHA3-384 and split with Shamir gf256
  case lamport_sha3_384_sig_share
      extends Multicodec("lamport-sha3-384-sig-share", "multisig", Array[Byte](0x1a.toByte, 0x55.toByte), DRAFT)
  // Lamport signature share based on SHA3-256 and split with Shamir gf256
  case lamport_sha3_256_sig_share
      extends Multicodec("lamport-sha3-256-sig-share", "multisig", Array[Byte](0x1a.toByte, 0x56.toByte), DRAFT)
  // KangarooTwelve is an extendable-output hash function based on Keccak-p
  case kangarootwelve extends Multicodec("kangarootwelve", "multihash", Array[Byte](0x1d.toByte, 0x01.toByte), DRAFT)
  // AES Galois/Counter Mode with 256-bit key and 12-byte IV
  case aes_gcm_256 extends Multicodec("aes-gcm-256", "encryption", Array[Byte](0x20.toByte, 0x00.toByte), DRAFT)
  // Experimental QUIC over yggdrasil and ironwood routing protocol
  case silverpine extends Multicodec("silverpine", "multiaddr", Array[Byte](0x3f.toByte, 0x42.toByte), DRAFT)
  case sm3_256 extends Multicodec("sm3-256", "multihash", Array[Byte](0x53.toByte, 0x4d.toByte), DRAFT)
  // The sum of multiple sha2-256 hashes; as specified by Ceramic CIP-124.
  case sha256a extends Multicodec("sha256a", "hash", Array[Byte](0x70.toByte, 0x12.toByte), DRAFT)
  // ChaCha20_Poly1305 encryption scheme
  case chacha20_poly1305
      extends Multicodec("chacha20-poly1305", "multikey", Array[Byte](0xa0.toByte, 0x00.toByte), DRAFT)
  // Blake2b consists of 64 output lengths that give different hashes
  case blake2b_8 extends Multicodec("blake2b-8", "multihash", Array[Byte](0xb2.toByte, 0x01.toByte), DRAFT)
  case blake2b_16 extends Multicodec("blake2b-16", "multihash", Array[Byte](0xb2.toByte, 0x02.toByte), DRAFT)
  case blake2b_24 extends Multicodec("blake2b-24", "multihash", Array[Byte](0xb2.toByte, 0x03.toByte), DRAFT)
  case blake2b_32 extends Multicodec("blake2b-32", "multihash", Array[Byte](0xb2.toByte, 0x04.toByte), DRAFT)
  case blake2b_40 extends Multicodec("blake2b-40", "multihash", Array[Byte](0xb2.toByte, 0x05.toByte), DRAFT)
  case blake2b_48 extends Multicodec("blake2b-48", "multihash", Array[Byte](0xb2.toByte, 0x06.toByte), DRAFT)
  case blake2b_56 extends Multicodec("blake2b-56", "multihash", Array[Byte](0xb2.toByte, 0x07.toByte), DRAFT)
  case blake2b_64 extends Multicodec("blake2b-64", "multihash", Array[Byte](0xb2.toByte, 0x08.toByte), DRAFT)
  case blake2b_72 extends Multicodec("blake2b-72", "multihash", Array[Byte](0xb2.toByte, 0x09.toByte), DRAFT)
  case blake2b_80 extends Multicodec("blake2b-80", "multihash", Array[Byte](0xb2.toByte, 0x0a.toByte), DRAFT)
  case blake2b_88 extends Multicodec("blake2b-88", "multihash", Array[Byte](0xb2.toByte, 0x0b.toByte), DRAFT)
  case blake2b_96 extends Multicodec("blake2b-96", "multihash", Array[Byte](0xb2.toByte, 0x0c.toByte), DRAFT)
  case blake2b_104 extends Multicodec("blake2b-104", "multihash", Array[Byte](0xb2.toByte, 0x0d.toByte), DRAFT)
  case blake2b_112 extends Multicodec("blake2b-112", "multihash", Array[Byte](0xb2.toByte, 0x0e.toByte), DRAFT)
  case blake2b_120 extends Multicodec("blake2b-120", "multihash", Array[Byte](0xb2.toByte, 0x0f.toByte), DRAFT)
  case blake2b_128 extends Multicodec("blake2b-128", "multihash", Array[Byte](0xb2.toByte, 0x10.toByte), DRAFT)
  case blake2b_136 extends Multicodec("blake2b-136", "multihash", Array[Byte](0xb2.toByte, 0x11.toByte), DRAFT)
  case blake2b_144 extends Multicodec("blake2b-144", "multihash", Array[Byte](0xb2.toByte, 0x12.toByte), DRAFT)
  case blake2b_152 extends Multicodec("blake2b-152", "multihash", Array[Byte](0xb2.toByte, 0x13.toByte), DRAFT)
  case blake2b_160 extends Multicodec("blake2b-160", "multihash", Array[Byte](0xb2.toByte, 0x14.toByte), DRAFT)
  case blake2b_168 extends Multicodec("blake2b-168", "multihash", Array[Byte](0xb2.toByte, 0x15.toByte), DRAFT)
  case blake2b_176 extends Multicodec("blake2b-176", "multihash", Array[Byte](0xb2.toByte, 0x16.toByte), DRAFT)
  case blake2b_184 extends Multicodec("blake2b-184", "multihash", Array[Byte](0xb2.toByte, 0x17.toByte), DRAFT)
  case blake2b_192 extends Multicodec("blake2b-192", "multihash", Array[Byte](0xb2.toByte, 0x18.toByte), DRAFT)
  case blake2b_200 extends Multicodec("blake2b-200", "multihash", Array[Byte](0xb2.toByte, 0x19.toByte), DRAFT)
  case blake2b_208 extends Multicodec("blake2b-208", "multihash", Array[Byte](0xb2.toByte, 0x1a.toByte), DRAFT)
  case blake2b_216 extends Multicodec("blake2b-216", "multihash", Array[Byte](0xb2.toByte, 0x1b.toByte), DRAFT)
  case blake2b_224 extends Multicodec("blake2b-224", "multihash", Array[Byte](0xb2.toByte, 0x1c.toByte), DRAFT)
  case blake2b_232 extends Multicodec("blake2b-232", "multihash", Array[Byte](0xb2.toByte, 0x1d.toByte), DRAFT)
  case blake2b_240 extends Multicodec("blake2b-240", "multihash", Array[Byte](0xb2.toByte, 0x1e.toByte), DRAFT)
  case blake2b_248 extends Multicodec("blake2b-248", "multihash", Array[Byte](0xb2.toByte, 0x1f.toByte), DRAFT)
  case blake2b_256 extends Multicodec("blake2b-256", "multihash", Array[Byte](0xb2.toByte, 0x20.toByte), PERMANENT)
  case blake2b_264 extends Multicodec("blake2b-264", "multihash", Array[Byte](0xb2.toByte, 0x21.toByte), DRAFT)
  case blake2b_272 extends Multicodec("blake2b-272", "multihash", Array[Byte](0xb2.toByte, 0x22.toByte), DRAFT)
  case blake2b_280 extends Multicodec("blake2b-280", "multihash", Array[Byte](0xb2.toByte, 0x23.toByte), DRAFT)
  case blake2b_288 extends Multicodec("blake2b-288", "multihash", Array[Byte](0xb2.toByte, 0x24.toByte), DRAFT)
  case blake2b_296 extends Multicodec("blake2b-296", "multihash", Array[Byte](0xb2.toByte, 0x25.toByte), DRAFT)
  case blake2b_304 extends Multicodec("blake2b-304", "multihash", Array[Byte](0xb2.toByte, 0x26.toByte), DRAFT)
  case blake2b_312 extends Multicodec("blake2b-312", "multihash", Array[Byte](0xb2.toByte, 0x27.toByte), DRAFT)
  case blake2b_320 extends Multicodec("blake2b-320", "multihash", Array[Byte](0xb2.toByte, 0x28.toByte), DRAFT)
  case blake2b_328 extends Multicodec("blake2b-328", "multihash", Array[Byte](0xb2.toByte, 0x29.toByte), DRAFT)
  case blake2b_336 extends Multicodec("blake2b-336", "multihash", Array[Byte](0xb2.toByte, 0x2a.toByte), DRAFT)
  case blake2b_344 extends Multicodec("blake2b-344", "multihash", Array[Byte](0xb2.toByte, 0x2b.toByte), DRAFT)
  case blake2b_352 extends Multicodec("blake2b-352", "multihash", Array[Byte](0xb2.toByte, 0x2c.toByte), DRAFT)
  case blake2b_360 extends Multicodec("blake2b-360", "multihash", Array[Byte](0xb2.toByte, 0x2d.toByte), DRAFT)
  case blake2b_368 extends Multicodec("blake2b-368", "multihash", Array[Byte](0xb2.toByte, 0x2e.toByte), DRAFT)
  case blake2b_376 extends Multicodec("blake2b-376", "multihash", Array[Byte](0xb2.toByte, 0x2f.toByte), DRAFT)
  case blake2b_384 extends Multicodec("blake2b-384", "multihash", Array[Byte](0xb2.toByte, 0x30.toByte), DRAFT)
  case blake2b_392 extends Multicodec("blake2b-392", "multihash", Array[Byte](0xb2.toByte, 0x31.toByte), DRAFT)
  case blake2b_400 extends Multicodec("blake2b-400", "multihash", Array[Byte](0xb2.toByte, 0x32.toByte), DRAFT)
  case blake2b_408 extends Multicodec("blake2b-408", "multihash", Array[Byte](0xb2.toByte, 0x33.toByte), DRAFT)
  case blake2b_416 extends Multicodec("blake2b-416", "multihash", Array[Byte](0xb2.toByte, 0x34.toByte), DRAFT)
  case blake2b_424 extends Multicodec("blake2b-424", "multihash", Array[Byte](0xb2.toByte, 0x35.toByte), DRAFT)
  case blake2b_432 extends Multicodec("blake2b-432", "multihash", Array[Byte](0xb2.toByte, 0x36.toByte), DRAFT)
  case blake2b_440 extends Multicodec("blake2b-440", "multihash", Array[Byte](0xb2.toByte, 0x37.toByte), DRAFT)
  case blake2b_448 extends Multicodec("blake2b-448", "multihash", Array[Byte](0xb2.toByte, 0x38.toByte), DRAFT)
  case blake2b_456 extends Multicodec("blake2b-456", "multihash", Array[Byte](0xb2.toByte, 0x39.toByte), DRAFT)
  case blake2b_464 extends Multicodec("blake2b-464", "multihash", Array[Byte](0xb2.toByte, 0x3a.toByte), DRAFT)
  case blake2b_472 extends Multicodec("blake2b-472", "multihash", Array[Byte](0xb2.toByte, 0x3b.toByte), DRAFT)
  case blake2b_480 extends Multicodec("blake2b-480", "multihash", Array[Byte](0xb2.toByte, 0x3c.toByte), DRAFT)
  case blake2b_488 extends Multicodec("blake2b-488", "multihash", Array[Byte](0xb2.toByte, 0x3d.toByte), DRAFT)
  case blake2b_496 extends Multicodec("blake2b-496", "multihash", Array[Byte](0xb2.toByte, 0x3e.toByte), DRAFT)
  case blake2b_504 extends Multicodec("blake2b-504", "multihash", Array[Byte](0xb2.toByte, 0x3f.toByte), DRAFT)
  case blake2b_512 extends Multicodec("blake2b-512", "multihash", Array[Byte](0xb2.toByte, 0x40.toByte), DRAFT)
  // Blake2s consists of 32 output lengths that give different hashes
  case blake2s_8 extends Multicodec("blake2s-8", "multihash", Array[Byte](0xb2.toByte, 0x41.toByte), DRAFT)
  case blake2s_16 extends Multicodec("blake2s-16", "multihash", Array[Byte](0xb2.toByte, 0x42.toByte), DRAFT)
  case blake2s_24 extends Multicodec("blake2s-24", "multihash", Array[Byte](0xb2.toByte, 0x43.toByte), DRAFT)
  case blake2s_32 extends Multicodec("blake2s-32", "multihash", Array[Byte](0xb2.toByte, 0x44.toByte), DRAFT)
  case blake2s_40 extends Multicodec("blake2s-40", "multihash", Array[Byte](0xb2.toByte, 0x45.toByte), DRAFT)
  case blake2s_48 extends Multicodec("blake2s-48", "multihash", Array[Byte](0xb2.toByte, 0x46.toByte), DRAFT)
  case blake2s_56 extends Multicodec("blake2s-56", "multihash", Array[Byte](0xb2.toByte, 0x47.toByte), DRAFT)
  case blake2s_64 extends Multicodec("blake2s-64", "multihash", Array[Byte](0xb2.toByte, 0x48.toByte), DRAFT)
  case blake2s_72 extends Multicodec("blake2s-72", "multihash", Array[Byte](0xb2.toByte, 0x49.toByte), DRAFT)
  case blake2s_80 extends Multicodec("blake2s-80", "multihash", Array[Byte](0xb2.toByte, 0x4a.toByte), DRAFT)
  case blake2s_88 extends Multicodec("blake2s-88", "multihash", Array[Byte](0xb2.toByte, 0x4b.toByte), DRAFT)
  case blake2s_96 extends Multicodec("blake2s-96", "multihash", Array[Byte](0xb2.toByte, 0x4c.toByte), DRAFT)
  case blake2s_104 extends Multicodec("blake2s-104", "multihash", Array[Byte](0xb2.toByte, 0x4d.toByte), DRAFT)
  case blake2s_112 extends Multicodec("blake2s-112", "multihash", Array[Byte](0xb2.toByte, 0x4e.toByte), DRAFT)
  case blake2s_120 extends Multicodec("blake2s-120", "multihash", Array[Byte](0xb2.toByte, 0x4f.toByte), DRAFT)
  case blake2s_128 extends Multicodec("blake2s-128", "multihash", Array[Byte](0xb2.toByte, 0x50.toByte), DRAFT)
  case blake2s_136 extends Multicodec("blake2s-136", "multihash", Array[Byte](0xb2.toByte, 0x51.toByte), DRAFT)
  case blake2s_144 extends Multicodec("blake2s-144", "multihash", Array[Byte](0xb2.toByte, 0x52.toByte), DRAFT)
  case blake2s_152 extends Multicodec("blake2s-152", "multihash", Array[Byte](0xb2.toByte, 0x53.toByte), DRAFT)
  case blake2s_160 extends Multicodec("blake2s-160", "multihash", Array[Byte](0xb2.toByte, 0x54.toByte), DRAFT)
  case blake2s_168 extends Multicodec("blake2s-168", "multihash", Array[Byte](0xb2.toByte, 0x55.toByte), DRAFT)
  case blake2s_176 extends Multicodec("blake2s-176", "multihash", Array[Byte](0xb2.toByte, 0x56.toByte), DRAFT)
  case blake2s_184 extends Multicodec("blake2s-184", "multihash", Array[Byte](0xb2.toByte, 0x57.toByte), DRAFT)
  case blake2s_192 extends Multicodec("blake2s-192", "multihash", Array[Byte](0xb2.toByte, 0x58.toByte), DRAFT)
  case blake2s_200 extends Multicodec("blake2s-200", "multihash", Array[Byte](0xb2.toByte, 0x59.toByte), DRAFT)
  case blake2s_208 extends Multicodec("blake2s-208", "multihash", Array[Byte](0xb2.toByte, 0x5a.toByte), DRAFT)
  case blake2s_216 extends Multicodec("blake2s-216", "multihash", Array[Byte](0xb2.toByte, 0x5b.toByte), DRAFT)
  case blake2s_224 extends Multicodec("blake2s-224", "multihash", Array[Byte](0xb2.toByte, 0x5c.toByte), DRAFT)
  case blake2s_232 extends Multicodec("blake2s-232", "multihash", Array[Byte](0xb2.toByte, 0x5d.toByte), DRAFT)
  case blake2s_240 extends Multicodec("blake2s-240", "multihash", Array[Byte](0xb2.toByte, 0x5e.toByte), DRAFT)
  case blake2s_248 extends Multicodec("blake2s-248", "multihash", Array[Byte](0xb2.toByte, 0x5f.toByte), DRAFT)
  case blake2s_256 extends Multicodec("blake2s-256", "multihash", Array[Byte](0xb2.toByte, 0x60.toByte), DRAFT)
  // Skein256 consists of 32 output lengths that give different hashes
  case skein256_8 extends Multicodec("skein256-8", "multihash", Array[Byte](0xb3.toByte, 0x01.toByte), DRAFT)
  case skein256_16 extends Multicodec("skein256-16", "multihash", Array[Byte](0xb3.toByte, 0x02.toByte), DRAFT)
  case skein256_24 extends Multicodec("skein256-24", "multihash", Array[Byte](0xb3.toByte, 0x03.toByte), DRAFT)
  case skein256_32 extends Multicodec("skein256-32", "multihash", Array[Byte](0xb3.toByte, 0x04.toByte), DRAFT)
  case skein256_40 extends Multicodec("skein256-40", "multihash", Array[Byte](0xb3.toByte, 0x05.toByte), DRAFT)
  case skein256_48 extends Multicodec("skein256-48", "multihash", Array[Byte](0xb3.toByte, 0x06.toByte), DRAFT)
  case skein256_56 extends Multicodec("skein256-56", "multihash", Array[Byte](0xb3.toByte, 0x07.toByte), DRAFT)
  case skein256_64 extends Multicodec("skein256-64", "multihash", Array[Byte](0xb3.toByte, 0x08.toByte), DRAFT)
  case skein256_72 extends Multicodec("skein256-72", "multihash", Array[Byte](0xb3.toByte, 0x09.toByte), DRAFT)
  case skein256_80 extends Multicodec("skein256-80", "multihash", Array[Byte](0xb3.toByte, 0x0a.toByte), DRAFT)
  case skein256_88 extends Multicodec("skein256-88", "multihash", Array[Byte](0xb3.toByte, 0x0b.toByte), DRAFT)
  case skein256_96 extends Multicodec("skein256-96", "multihash", Array[Byte](0xb3.toByte, 0x0c.toByte), DRAFT)
  case skein256_104 extends Multicodec("skein256-104", "multihash", Array[Byte](0xb3.toByte, 0x0d.toByte), DRAFT)
  case skein256_112 extends Multicodec("skein256-112", "multihash", Array[Byte](0xb3.toByte, 0x0e.toByte), DRAFT)
  case skein256_120 extends Multicodec("skein256-120", "multihash", Array[Byte](0xb3.toByte, 0x0f.toByte), DRAFT)
  case skein256_128 extends Multicodec("skein256-128", "multihash", Array[Byte](0xb3.toByte, 0x10.toByte), DRAFT)
  case skein256_136 extends Multicodec("skein256-136", "multihash", Array[Byte](0xb3.toByte, 0x11.toByte), DRAFT)
  case skein256_144 extends Multicodec("skein256-144", "multihash", Array[Byte](0xb3.toByte, 0x12.toByte), DRAFT)
  case skein256_152 extends Multicodec("skein256-152", "multihash", Array[Byte](0xb3.toByte, 0x13.toByte), DRAFT)
  case skein256_160 extends Multicodec("skein256-160", "multihash", Array[Byte](0xb3.toByte, 0x14.toByte), DRAFT)
  case skein256_168 extends Multicodec("skein256-168", "multihash", Array[Byte](0xb3.toByte, 0x15.toByte), DRAFT)
  case skein256_176 extends Multicodec("skein256-176", "multihash", Array[Byte](0xb3.toByte, 0x16.toByte), DRAFT)
  case skein256_184 extends Multicodec("skein256-184", "multihash", Array[Byte](0xb3.toByte, 0x17.toByte), DRAFT)
  case skein256_192 extends Multicodec("skein256-192", "multihash", Array[Byte](0xb3.toByte, 0x18.toByte), DRAFT)
  case skein256_200 extends Multicodec("skein256-200", "multihash", Array[Byte](0xb3.toByte, 0x19.toByte), DRAFT)
  case skein256_208 extends Multicodec("skein256-208", "multihash", Array[Byte](0xb3.toByte, 0x1a.toByte), DRAFT)
  case skein256_216 extends Multicodec("skein256-216", "multihash", Array[Byte](0xb3.toByte, 0x1b.toByte), DRAFT)
  case skein256_224 extends Multicodec("skein256-224", "multihash", Array[Byte](0xb3.toByte, 0x1c.toByte), DRAFT)
  case skein256_232 extends Multicodec("skein256-232", "multihash", Array[Byte](0xb3.toByte, 0x1d.toByte), DRAFT)
  case skein256_240 extends Multicodec("skein256-240", "multihash", Array[Byte](0xb3.toByte, 0x1e.toByte), DRAFT)
  case skein256_248 extends Multicodec("skein256-248", "multihash", Array[Byte](0xb3.toByte, 0x1f.toByte), DRAFT)
  case skein256_256 extends Multicodec("skein256-256", "multihash", Array[Byte](0xb3.toByte, 0x20.toByte), DRAFT)
// Skein512 consists of 64 output lengths that give different hashes
  case skein512_8 extends Multicodec("skein512-8", "multihash", Array[Byte](0xb3.toByte, 0x21.toByte), DRAFT)
  case skein512_16 extends Multicodec("skein512-16", "multihash", Array[Byte](0xb3.toByte, 0x22.toByte), DRAFT)
  case skein512_24 extends Multicodec("skein512-24", "multihash", Array[Byte](0xb3.toByte, 0x23.toByte), DRAFT)
  case skein512_32 extends Multicodec("skein512-32", "multihash", Array[Byte](0xb3.toByte, 0x24.toByte), DRAFT)
  case skein512_40 extends Multicodec("skein512-40", "multihash", Array[Byte](0xb3.toByte, 0x25.toByte), DRAFT)
  case skein512_48 extends Multicodec("skein512-48", "multihash", Array[Byte](0xb3.toByte, 0x26.toByte), DRAFT)
  case skein512_56 extends Multicodec("skein512-56", "multihash", Array[Byte](0xb3.toByte, 0x27.toByte), DRAFT)
  case skein512_64 extends Multicodec("skein512-64", "multihash", Array[Byte](0xb3.toByte, 0x28.toByte), DRAFT)
  case skein512_72 extends Multicodec("skein512-72", "multihash", Array[Byte](0xb3.toByte, 0x29.toByte), DRAFT)
  case skein512_80 extends Multicodec("skein512-80", "multihash", Array[Byte](0xb3.toByte, 0x2a.toByte), DRAFT)
  case skein512_88 extends Multicodec("skein512-88", "multihash", Array[Byte](0xb3.toByte, 0x2b.toByte), DRAFT)
  case skein512_96 extends Multicodec("skein512-96", "multihash", Array[Byte](0xb3.toByte, 0x2c.toByte), DRAFT)
  case skein512_104 extends Multicodec("skein512-104", "multihash", Array[Byte](0xb3.toByte, 0x2d.toByte), DRAFT)
  case skein512_112 extends Multicodec("skein512-112", "multihash", Array[Byte](0xb3.toByte, 0x2e.toByte), DRAFT)
  case skein512_120 extends Multicodec("skein512-120", "multihash", Array[Byte](0xb3.toByte, 0x2f.toByte), DRAFT)
  case skein512_128 extends Multicodec("skein512-128", "multihash", Array[Byte](0xb3.toByte, 0x30.toByte), DRAFT)
  case skein512_136 extends Multicodec("skein512-136", "multihash", Array[Byte](0xb3.toByte, 0x31.toByte), DRAFT)
  case skein512_144 extends Multicodec("skein512-144", "multihash", Array[Byte](0xb3.toByte, 0x32.toByte), DRAFT)
  case skein512_152 extends Multicodec("skein512-152", "multihash", Array[Byte](0xb3.toByte, 0x33.toByte), DRAFT)
  case skein512_160 extends Multicodec("skein512-160", "multihash", Array[Byte](0xb3.toByte, 0x34.toByte), DRAFT)
  case skein512_168 extends Multicodec("skein512-168", "multihash", Array[Byte](0xb3.toByte, 0x35.toByte), DRAFT)
  case skein512_176 extends Multicodec("skein512-176", "multihash", Array[Byte](0xb3.toByte, 0x36.toByte), DRAFT)
  case skein512_184 extends Multicodec("skein512-184", "multihash", Array[Byte](0xb3.toByte, 0x37.toByte), DRAFT)
  case skein512_192 extends Multicodec("skein512-192", "multihash", Array[Byte](0xb3.toByte, 0x38.toByte), DRAFT)
  case skein512_200 extends Multicodec("skein512-200", "multihash", Array[Byte](0xb3.toByte, 0x39.toByte), DRAFT)
  case skein512_208 extends Multicodec("skein512-208", "multihash", Array[Byte](0xb3.toByte, 0x3a.toByte), DRAFT)
  case skein512_216 extends Multicodec("skein512-216", "multihash", Array[Byte](0xb3.toByte, 0x3b.toByte), DRAFT)
  case skein512_224 extends Multicodec("skein512-224", "multihash", Array[Byte](0xb3.toByte, 0x3c.toByte), DRAFT)
  case skein512_232 extends Multicodec("skein512-232", "multihash", Array[Byte](0xb3.toByte, 0x3d.toByte), DRAFT)
  case skein512_240 extends Multicodec("skein512-240", "multihash", Array[Byte](0xb3.toByte, 0x3e.toByte), DRAFT)
  case skein512_248 extends Multicodec("skein512-248", "multihash", Array[Byte](0xb3.toByte, 0x3f.toByte), DRAFT)
  case skein512_256 extends Multicodec("skein512-256", "multihash", Array[Byte](0xb3.toByte, 0x40.toByte), DRAFT)
  case skein512_264 extends Multicodec("skein512-264", "multihash", Array[Byte](0xb3.toByte, 0x41.toByte), DRAFT)
  case skein512_272 extends Multicodec("skein512-272", "multihash", Array[Byte](0xb3.toByte, 0x42.toByte), DRAFT)
  case skein512_280 extends Multicodec("skein512-280", "multihash", Array[Byte](0xb3.toByte, 0x43.toByte), DRAFT)
  case skein512_288 extends Multicodec("skein512-288", "multihash", Array[Byte](0xb3.toByte, 0x44.toByte), DRAFT)
  case skein512_296 extends Multicodec("skein512-296", "multihash", Array[Byte](0xb3.toByte, 0x45.toByte), DRAFT)
  case skein512_304 extends Multicodec("skein512-304", "multihash", Array[Byte](0xb3.toByte, 0x46.toByte), DRAFT)
  case skein512_312 extends Multicodec("skein512-312", "multihash", Array[Byte](0xb3.toByte, 0x47.toByte), DRAFT)
  case skein512_320 extends Multicodec("skein512-320", "multihash", Array[Byte](0xb3.toByte, 0x48.toByte), DRAFT)
  case skein512_328 extends Multicodec("skein512-328", "multihash", Array[Byte](0xb3.toByte, 0x49.toByte), DRAFT)
  case skein512_336 extends Multicodec("skein512-336", "multihash", Array[Byte](0xb3.toByte, 0x4a.toByte), DRAFT)
  case skein512_344 extends Multicodec("skein512-344", "multihash", Array[Byte](0xb3.toByte, 0x4b.toByte), DRAFT)
  case skein512_352 extends Multicodec("skein512-352", "multihash", Array[Byte](0xb3.toByte, 0x4c.toByte), DRAFT)
  case skein512_360 extends Multicodec("skein512-360", "multihash", Array[Byte](0xb3.toByte, 0x4d.toByte), DRAFT)
  case skein512_368 extends Multicodec("skein512-368", "multihash", Array[Byte](0xb3.toByte, 0x4e.toByte), DRAFT)
  case skein512_376 extends Multicodec("skein512-376", "multihash", Array[Byte](0xb3.toByte, 0x4f.toByte), DRAFT)
  case skein512_384 extends Multicodec("skein512-384", "multihash", Array[Byte](0xb3.toByte, 0x50.toByte), DRAFT)
  case skein512_392 extends Multicodec("skein512-392", "multihash", Array[Byte](0xb3.toByte, 0x51.toByte), DRAFT)
  case skein512_400 extends Multicodec("skein512-400", "multihash", Array[Byte](0xb3.toByte, 0x52.toByte), DRAFT)
  case skein512_408 extends Multicodec("skein512-408", "multihash", Array[Byte](0xb3.toByte, 0x53.toByte), DRAFT)
  case skein512_416 extends Multicodec("skein512-416", "multihash", Array[Byte](0xb3.toByte, 0x54.toByte), DRAFT)
  case skein512_424 extends Multicodec("skein512-424", "multihash", Array[Byte](0xb3.toByte, 0x55.toByte), DRAFT)
  case skein512_432 extends Multicodec("skein512-432", "multihash", Array[Byte](0xb3.toByte, 0x56.toByte), DRAFT)
  case skein512_440 extends Multicodec("skein512-440", "multihash", Array[Byte](0xb3.toByte, 0x57.toByte), DRAFT)
  case skein512_448 extends Multicodec("skein512-448", "multihash", Array[Byte](0xb3.toByte, 0x58.toByte), DRAFT)
  case skein512_456 extends Multicodec("skein512-456", "multihash", Array[Byte](0xb3.toByte, 0x59.toByte), DRAFT)
  case skein512_464 extends Multicodec("skein512-464", "multihash", Array[Byte](0xb3.toByte, 0x5a.toByte), DRAFT)
  case skein512_472 extends Multicodec("skein512-472", "multihash", Array[Byte](0xb3.toByte, 0x5b.toByte), DRAFT)
  case skein512_480 extends Multicodec("skein512-480", "multihash", Array[Byte](0xb3.toByte, 0x5c.toByte), DRAFT)
  case skein512_488 extends Multicodec("skein512-488", "multihash", Array[Byte](0xb3.toByte, 0x5d.toByte), DRAFT)
  case skein512_496 extends Multicodec("skein512-496", "multihash", Array[Byte](0xb3.toByte, 0x5e.toByte), DRAFT)
  case skein512_504 extends Multicodec("skein512-504", "multihash", Array[Byte](0xb3.toByte, 0x5f.toByte), DRAFT)
  case skein512_512 extends Multicodec("skein512-512", "multihash", Array[Byte](0xb3.toByte, 0x60.toByte), DRAFT)
  // Skein1024 consists of 128 output lengths that give different hashes
  case skein1024_8 extends Multicodec("skein1024-8", "multihash", Array[Byte](0xb3.toByte, 0x61.toByte), DRAFT)
  case skein1024_16 extends Multicodec("skein1024-16", "multihash", Array[Byte](0xb3.toByte, 0x62.toByte), DRAFT)
  case skein1024_24 extends Multicodec("skein1024-24", "multihash", Array[Byte](0xb3.toByte, 0x63.toByte), DRAFT)
  case skein1024_32 extends Multicodec("skein1024-32", "multihash", Array[Byte](0xb3.toByte, 0x64.toByte), DRAFT)
  case skein1024_40 extends Multicodec("skein1024-40", "multihash", Array[Byte](0xb3.toByte, 0x65.toByte), DRAFT)
  case skein1024_48 extends Multicodec("skein1024-48", "multihash", Array[Byte](0xb3.toByte, 0x66.toByte), DRAFT)
  case skein1024_56 extends Multicodec("skein1024-56", "multihash", Array[Byte](0xb3.toByte, 0x67.toByte), DRAFT)
  case skein1024_64 extends Multicodec("skein1024-64", "multihash", Array[Byte](0xb3.toByte, 0x68.toByte), DRAFT)
  case skein1024_72 extends Multicodec("skein1024-72", "multihash", Array[Byte](0xb3.toByte, 0x69.toByte), DRAFT)
  case skein1024_80 extends Multicodec("skein1024-80", "multihash", Array[Byte](0xb3.toByte, 0x6a.toByte), DRAFT)
  case skein1024_88 extends Multicodec("skein1024-88", "multihash", Array[Byte](0xb3.toByte, 0x6b.toByte), DRAFT)
  case skein1024_96 extends Multicodec("skein1024-96", "multihash", Array[Byte](0xb3.toByte, 0x6c.toByte), DRAFT)
  case skein1024_104 extends Multicodec("skein1024-104", "multihash", Array[Byte](0xb3.toByte, 0x6d.toByte), DRAFT)
  case skein1024_112 extends Multicodec("skein1024-112", "multihash", Array[Byte](0xb3.toByte, 0x6e.toByte), DRAFT)
  case skein1024_120 extends Multicodec("skein1024-120", "multihash", Array[Byte](0xb3.toByte, 0x6f.toByte), DRAFT)
  case skein1024_128 extends Multicodec("skein1024-128", "multihash", Array[Byte](0xb3.toByte, 0x70.toByte), DRAFT)
  case skein1024_136 extends Multicodec("skein1024-136", "multihash", Array[Byte](0xb3.toByte, 0x71.toByte), DRAFT)
  case skein1024_144 extends Multicodec("skein1024-144", "multihash", Array[Byte](0xb3.toByte, 0x72.toByte), DRAFT)
  case skein1024_152 extends Multicodec("skein1024-152", "multihash", Array[Byte](0xb3.toByte, 0x73.toByte), DRAFT)
  case skein1024_160 extends Multicodec("skein1024-160", "multihash", Array[Byte](0xb3.toByte, 0x74.toByte), DRAFT)
  case skein1024_168 extends Multicodec("skein1024-168", "multihash", Array[Byte](0xb3.toByte, 0x75.toByte), DRAFT)
  case skein1024_176 extends Multicodec("skein1024-176", "multihash", Array[Byte](0xb3.toByte, 0x76.toByte), DRAFT)
  case skein1024_184 extends Multicodec("skein1024-184", "multihash", Array[Byte](0xb3.toByte, 0x77.toByte), DRAFT)
  case skein1024_192 extends Multicodec("skein1024-192", "multihash", Array[Byte](0xb3.toByte, 0x78.toByte), DRAFT)
  case skein1024_200 extends Multicodec("skein1024-200", "multihash", Array[Byte](0xb3.toByte, 0x79.toByte), DRAFT)
  case skein1024_208 extends Multicodec("skein1024-208", "multihash", Array[Byte](0xb3.toByte, 0x7a.toByte), DRAFT)
  case skein1024_216 extends Multicodec("skein1024-216", "multihash", Array[Byte](0xb3.toByte, 0x7b.toByte), DRAFT)
  case skein1024_224 extends Multicodec("skein1024-224", "multihash", Array[Byte](0xb3.toByte, 0x7c.toByte), DRAFT)
  case skein1024_232 extends Multicodec("skein1024-232", "multihash", Array[Byte](0xb3.toByte, 0x7d.toByte), DRAFT)
  case skein1024_240 extends Multicodec("skein1024-240", "multihash", Array[Byte](0xb3.toByte, 0x7e.toByte), DRAFT)
  case skein1024_248 extends Multicodec("skein1024-248", "multihash", Array[Byte](0xb3.toByte, 0x7f.toByte), DRAFT)
  case skein1024_256 extends Multicodec("skein1024-256", "multihash", Array[Byte](0xb3.toByte, 0x80.toByte), DRAFT)
  case skein1024_264 extends Multicodec("skein1024-264", "multihash", Array[Byte](0xb3.toByte, 0x81.toByte), DRAFT)
  case skein1024_272 extends Multicodec("skein1024-272", "multihash", Array[Byte](0xb3.toByte, 0x82.toByte), DRAFT)
  case skein1024_280 extends Multicodec("skein1024-280", "multihash", Array[Byte](0xb3.toByte, 0x83.toByte), DRAFT)
  case skein1024_288 extends Multicodec("skein1024-288", "multihash", Array[Byte](0xb3.toByte, 0x84.toByte), DRAFT)
  case skein1024_296 extends Multicodec("skein1024-296", "multihash", Array[Byte](0xb3.toByte, 0x85.toByte), DRAFT)
  case skein1024_304 extends Multicodec("skein1024-304", "multihash", Array[Byte](0xb3.toByte, 0x86.toByte), DRAFT)
  case skein1024_312 extends Multicodec("skein1024-312", "multihash", Array[Byte](0xb3.toByte, 0x87.toByte), DRAFT)
  case skein1024_320 extends Multicodec("skein1024-320", "multihash", Array[Byte](0xb3.toByte, 0x88.toByte), DRAFT)
  case skein1024_328 extends Multicodec("skein1024-328", "multihash", Array[Byte](0xb3.toByte, 0x89.toByte), DRAFT)
  case skein1024_336 extends Multicodec("skein1024-336", "multihash", Array[Byte](0xb3.toByte, 0x8a.toByte), DRAFT)
  case skein1024_344 extends Multicodec("skein1024-344", "multihash", Array[Byte](0xb3.toByte, 0x8b.toByte), DRAFT)
  case skein1024_352 extends Multicodec("skein1024-352", "multihash", Array[Byte](0xb3.toByte, 0x8c.toByte), DRAFT)
  case skein1024_360 extends Multicodec("skein1024-360", "multihash", Array[Byte](0xb3.toByte, 0x8d.toByte), DRAFT)
  case skein1024_368 extends Multicodec("skein1024-368", "multihash", Array[Byte](0xb3.toByte, 0x8e.toByte), DRAFT)
  case skein1024_376 extends Multicodec("skein1024-376", "multihash", Array[Byte](0xb3.toByte, 0x8f.toByte), DRAFT)
  case skein1024_384 extends Multicodec("skein1024-384", "multihash", Array[Byte](0xb3.toByte, 0x90.toByte), DRAFT)
  case skein1024_392 extends Multicodec("skein1024-392", "multihash", Array[Byte](0xb3.toByte, 0x91.toByte), DRAFT)
  case skein1024_400 extends Multicodec("skein1024-400", "multihash", Array[Byte](0xb3.toByte, 0x92.toByte), DRAFT)
  case skein1024_408 extends Multicodec("skein1024-408", "multihash", Array[Byte](0xb3.toByte, 0x93.toByte), DRAFT)
  case skein1024_416 extends Multicodec("skein1024-416", "multihash", Array[Byte](0xb3.toByte, 0x94.toByte), DRAFT)
  case skein1024_424 extends Multicodec("skein1024-424", "multihash", Array[Byte](0xb3.toByte, 0x95.toByte), DRAFT)
  case skein1024_432 extends Multicodec("skein1024-432", "multihash", Array[Byte](0xb3.toByte, 0x96.toByte), DRAFT)
  case skein1024_440 extends Multicodec("skein1024-440", "multihash", Array[Byte](0xb3.toByte, 0x97.toByte), DRAFT)
  case skein1024_448 extends Multicodec("skein1024-448", "multihash", Array[Byte](0xb3.toByte, 0x98.toByte), DRAFT)
  case skein1024_456 extends Multicodec("skein1024-456", "multihash", Array[Byte](0xb3.toByte, 0x99.toByte), DRAFT)
  case skein1024_464 extends Multicodec("skein1024-464", "multihash", Array[Byte](0xb3.toByte, 0x9a.toByte), DRAFT)
  case skein1024_472 extends Multicodec("skein1024-472", "multihash", Array[Byte](0xb3.toByte, 0x9b.toByte), DRAFT)
  case skein1024_480 extends Multicodec("skein1024-480", "multihash", Array[Byte](0xb3.toByte, 0x9c.toByte), DRAFT)
  case skein1024_488 extends Multicodec("skein1024-488", "multihash", Array[Byte](0xb3.toByte, 0x9d.toByte), DRAFT)
  case skein1024_496 extends Multicodec("skein1024-496", "multihash", Array[Byte](0xb3.toByte, 0x9e.toByte), DRAFT)
  case skein1024_504 extends Multicodec("skein1024-504", "multihash", Array[Byte](0xb3.toByte, 0x9f.toByte), DRAFT)
  case skein1024_512 extends Multicodec("skein1024-512", "multihash", Array[Byte](0xb3.toByte, 0xa0.toByte), DRAFT)
  case skein1024_520 extends Multicodec("skein1024-520", "multihash", Array[Byte](0xb3.toByte, 0xa1.toByte), DRAFT)
  case skein1024_528 extends Multicodec("skein1024-528", "multihash", Array[Byte](0xb3.toByte, 0xa2.toByte), DRAFT)
  case skein1024_536 extends Multicodec("skein1024-536", "multihash", Array[Byte](0xb3.toByte, 0xa3.toByte), DRAFT)
  case skein1024_544 extends Multicodec("skein1024-544", "multihash", Array[Byte](0xb3.toByte, 0xa4.toByte), DRAFT)
  case skein1024_552 extends Multicodec("skein1024-552", "multihash", Array[Byte](0xb3.toByte, 0xa5.toByte), DRAFT)
  case skein1024_560 extends Multicodec("skein1024-560", "multihash", Array[Byte](0xb3.toByte, 0xa6.toByte), DRAFT)
  case skein1024_568 extends Multicodec("skein1024-568", "multihash", Array[Byte](0xb3.toByte, 0xa7.toByte), DRAFT)
  case skein1024_576 extends Multicodec("skein1024-576", "multihash", Array[Byte](0xb3.toByte, 0xa8.toByte), DRAFT)
  case skein1024_584 extends Multicodec("skein1024-584", "multihash", Array[Byte](0xb3.toByte, 0xa9.toByte), DRAFT)
  case skein1024_592 extends Multicodec("skein1024-592", "multihash", Array[Byte](0xb3.toByte, 0xaa.toByte), DRAFT)
  case skein1024_600 extends Multicodec("skein1024-600", "multihash", Array[Byte](0xb3.toByte, 0xab.toByte), DRAFT)
  case skein1024_608 extends Multicodec("skein1024-608", "multihash", Array[Byte](0xb3.toByte, 0xac.toByte), DRAFT)
  case skein1024_616 extends Multicodec("skein1024-616", "multihash", Array[Byte](0xb3.toByte, 0xad.toByte), DRAFT)
  case skein1024_624 extends Multicodec("skein1024-624", "multihash", Array[Byte](0xb3.toByte, 0xae.toByte), DRAFT)
  case skein1024_632 extends Multicodec("skein1024-632", "multihash", Array[Byte](0xb3.toByte, 0xaf.toByte), DRAFT)
  case skein1024_640 extends Multicodec("skein1024-640", "multihash", Array[Byte](0xb3.toByte, 0xb0.toByte), DRAFT)
  case skein1024_648 extends Multicodec("skein1024-648", "multihash", Array[Byte](0xb3.toByte, 0xb1.toByte), DRAFT)
  case skein1024_656 extends Multicodec("skein1024-656", "multihash", Array[Byte](0xb3.toByte, 0xb2.toByte), DRAFT)
  case skein1024_664 extends Multicodec("skein1024-664", "multihash", Array[Byte](0xb3.toByte, 0xb3.toByte), DRAFT)
  case skein1024_672 extends Multicodec("skein1024-672", "multihash", Array[Byte](0xb3.toByte, 0xb4.toByte), DRAFT)
  case skein1024_680 extends Multicodec("skein1024-680", "multihash", Array[Byte](0xb3.toByte, 0xb5.toByte), DRAFT)
  case skein1024_688 extends Multicodec("skein1024-688", "multihash", Array[Byte](0xb3.toByte, 0xb6.toByte), DRAFT)
  case skein1024_696 extends Multicodec("skein1024-696", "multihash", Array[Byte](0xb3.toByte, 0xb7.toByte), DRAFT)
  case skein1024_704 extends Multicodec("skein1024-704", "multihash", Array[Byte](0xb3.toByte, 0xb8.toByte), DRAFT)
  case skein1024_712 extends Multicodec("skein1024-712", "multihash", Array[Byte](0xb3.toByte, 0xb9.toByte), DRAFT)
  case skein1024_720 extends Multicodec("skein1024-720", "multihash", Array[Byte](0xb3.toByte, 0xba.toByte), DRAFT)
  case skein1024_728 extends Multicodec("skein1024-728", "multihash", Array[Byte](0xb3.toByte, 0xbb.toByte), DRAFT)
  case skein1024_736 extends Multicodec("skein1024-736", "multihash", Array[Byte](0xb3.toByte, 0xbc.toByte), DRAFT)
  case skein1024_744 extends Multicodec("skein1024-744", "multihash", Array[Byte](0xb3.toByte, 0xbd.toByte), DRAFT)
  case skein1024_752 extends Multicodec("skein1024-752", "multihash", Array[Byte](0xb3.toByte, 0xbe.toByte), DRAFT)
  case skein1024_760 extends Multicodec("skein1024-760", "multihash", Array[Byte](0xb3.toByte, 0xbf.toByte), DRAFT)
  case skein1024_768 extends Multicodec("skein1024-768", "multihash", Array[Byte](0xb3.toByte, 0xc0.toByte), DRAFT)
  case skein1024_776 extends Multicodec("skein1024-776", "multihash", Array[Byte](0xb3.toByte, 0xc1.toByte), DRAFT)
  case skein1024_784 extends Multicodec("skein1024-784", "multihash", Array[Byte](0xb3.toByte, 0xc2.toByte), DRAFT)
  case skein1024_792 extends Multicodec("skein1024-792", "multihash", Array[Byte](0xb3.toByte, 0xc3.toByte), DRAFT)
  case skein1024_800 extends Multicodec("skein1024-800", "multihash", Array[Byte](0xb3.toByte, 0xc4.toByte), DRAFT)
  case skein1024_808 extends Multicodec("skein1024-808", "multihash", Array[Byte](0xb3.toByte, 0xc5.toByte), DRAFT)
  case skein1024_816 extends Multicodec("skein1024-816", "multihash", Array[Byte](0xb3.toByte, 0xc6.toByte), DRAFT)
  case skein1024_824 extends Multicodec("skein1024-824", "multihash", Array[Byte](0xb3.toByte, 0xc7.toByte), DRAFT)
  case skein1024_832 extends Multicodec("skein1024-832", "multihash", Array[Byte](0xb3.toByte, 0xc8.toByte), DRAFT)
  case skein1024_840 extends Multicodec("skein1024-840", "multihash", Array[Byte](0xb3.toByte, 0xc9.toByte), DRAFT)
  case skein1024_848 extends Multicodec("skein1024-848", "multihash", Array[Byte](0xb3.toByte, 0xca.toByte), DRAFT)
  case skein1024_856 extends Multicodec("skein1024-856", "multihash", Array[Byte](0xb3.toByte, 0xcb.toByte), DRAFT)
  case skein1024_864 extends Multicodec("skein1024-864", "multihash", Array[Byte](0xb3.toByte, 0xcc.toByte), DRAFT)
  case skein1024_872 extends Multicodec("skein1024-872", "multihash", Array[Byte](0xb3.toByte, 0xcd.toByte), DRAFT)
  case skein1024_880 extends Multicodec("skein1024-880", "multihash", Array[Byte](0xb3.toByte, 0xce.toByte), DRAFT)
  case skein1024_888 extends Multicodec("skein1024-888", "multihash", Array[Byte](0xb3.toByte, 0xcf.toByte), DRAFT)
  case skein1024_896 extends Multicodec("skein1024-896", "multihash", Array[Byte](0xb3.toByte, 0xd0.toByte), DRAFT)
  case skein1024_904 extends Multicodec("skein1024-904", "multihash", Array[Byte](0xb3.toByte, 0xd1.toByte), DRAFT)
  case skein1024_912 extends Multicodec("skein1024-912", "multihash", Array[Byte](0xb3.toByte, 0xd2.toByte), DRAFT)
  case skein1024_920 extends Multicodec("skein1024-920", "multihash", Array[Byte](0xb3.toByte, 0xd3.toByte), DRAFT)
  case skein1024_928 extends Multicodec("skein1024-928", "multihash", Array[Byte](0xb3.toByte, 0xd4.toByte), DRAFT)
  case skein1024_936 extends Multicodec("skein1024-936", "multihash", Array[Byte](0xb3.toByte, 0xd5.toByte), DRAFT)
  case skein1024_944 extends Multicodec("skein1024-944", "multihash", Array[Byte](0xb3.toByte, 0xd6.toByte), DRAFT)
  case skein1024_952 extends Multicodec("skein1024-952", "multihash", Array[Byte](0xb3.toByte, 0xd7.toByte), DRAFT)
  case skein1024_960 extends Multicodec("skein1024-960", "multihash", Array[Byte](0xb3.toByte, 0xd8.toByte), DRAFT)
  case skein1024_968 extends Multicodec("skein1024-968", "multihash", Array[Byte](0xb3.toByte, 0xd9.toByte), DRAFT)
  case skein1024_976 extends Multicodec("skein1024-976", "multihash", Array[Byte](0xb3.toByte, 0xda.toByte), DRAFT)
  case skein1024_984 extends Multicodec("skein1024-984", "multihash", Array[Byte](0xb3.toByte, 0xdb.toByte), DRAFT)
  case skein1024_992 extends Multicodec("skein1024-992", "multihash", Array[Byte](0xb3.toByte, 0xdc.toByte), DRAFT)
  case skein1024_1000 extends Multicodec("skein1024-1000", "multihash", Array[Byte](0xb3.toByte, 0xdd.toByte), DRAFT)
  case skein1024_1008 extends Multicodec("skein1024-1008", "multihash", Array[Byte](0xb3.toByte, 0xde.toByte), DRAFT)
  case skein1024_1016 extends Multicodec("skein1024-1016", "multihash", Array[Byte](0xb3.toByte, 0xdf.toByte), DRAFT)
  case skein1024_1024 extends Multicodec("skein1024-1024", "multihash", Array[Byte](0xb3.toByte, 0xe0.toByte), DRAFT)
  // Extremely fast non-cryptographic hash algorithm "")
  case xxh_32 extends Multicodec("xxh-32", "hash", Array[Byte](0xb3.toByte, 0xe1.toByte), DRAFT)
  // Extremely fast non-cryptographic hash algorithm "")
  case xxh_64 extends Multicodec("xxh-64", "hash", Array[Byte](0xb3.toByte, 0xe2.toByte), DRAFT)
  // Extremely fast non-cryptographic hash algorithm "")
  case xxh3_64 extends Multicodec("xxh3-64", "hash", Array[Byte](0xb3.toByte, 0xe3.toByte), DRAFT)
  // Extremely fast non-cryptographic hash algorithm "")
  case xxh3_128 extends Multicodec("xxh3-128", "hash", Array[Byte](0xb3.toByte, 0xe4.toByte), DRAFT)
  // Poseidon using BLS12-381 and arity of 2 with Filecoin parameters "")
  case poseidon_bls12_381_a2_fc1
      extends Multicodec("poseidon-bls12_381-a2-fc1", "multihash", Array[Byte](0xb4.toByte, 0x01.toByte), PERMANENT)
  // Poseidon using BLS12-381 and arity of 2 with Filecoin parameters - high-security variant "")
  case poseidon_bls12_381_a2_fc1_sc
      extends Multicodec("poseidon-bls12_381-a2-fc1-sc", "multihash", Array[Byte](0xb4.toByte, 0x02.toByte), DRAFT)
  // The result of canonicalizing an input according to RDFC-1.0 and then expressing its hash value as a multihash value. "")
  case rdfc_1 extends Multicodec("rdfc-1", "ipld", Array[Byte](0xb4.toByte, 0x03.toByte), DRAFT)
  // SimpleSerialize (SSZ) serialization "")
  case ssz extends Multicodec("ssz", "serialization", Array[Byte](0xb5.toByte, 0x01.toByte), DRAFT)
  // SSZ Merkle tree root using SHA2-256 as the hashing function and SSZ serialization for the block binary "")
  case ssz_sha2_256_bmt
      extends Multicodec("ssz-sha2-256-bmt", "multihash", Array[Byte](0xb5.toByte, 0x02.toByte), DRAFT)
  // Hash of concatenated SHA2-256 digests of 8*2^n MiB source chunks; n = ceil(log2(source_size/(10^4 * 8MiB))) "")
  case sha2_256_chunked
      extends Multicodec("sha2-256-chunked", "multihash", Array[Byte](0xb5.toByte, 0x10.toByte), DRAFT)
  // The result of canonicalizing an input according to JCS - JSON Canonicalisation Scheme (RFC 8785) "")
  case json_jcs extends Multicodec("json-jcs", "ipld", Array[Byte](0xb6.toByte, 0x01.toByte), DRAFT)
  // ISCC (International Standard Content Code) - similarity preserving hash "")
  case iscc extends Multicodec("iscc", "softhash", Array[Byte](0xcc.toByte, 0x01.toByte), DRAFT)
  // 0xcert Asset Imprint (root hash) "")
  case zeroxcert_imprint_256
      extends Multicodec("zeroxcert-imprint-256", "zeroxcert", Array[Byte](0xce.toByte, 0x11.toByte), DRAFT)
  // Namespace for all not yet standard signature algorithms "")
  case nonstandard_sig
      extends Multicodec("nonstandard-sig", "varsig", Array[Byte](0xd0.toByte, 0x00.toByte), DEPRECATED)
  // Bcrypt-PBKDF key derivation function "")
  case bcrypt_pbkdf extends Multicodec("bcrypt-pbkdf", "multihash", Array[Byte](0xd0.toByte, 0x0d.toByte), DRAFT)
  // ES256K Siganture Algorithm (secp256k1) "")
  case es256k extends Multicodec("es256k", "varsig", Array[Byte](0xd0.toByte, 0xe7.toByte), DRAFT)
  // G1 signature for BLS12-381 "")
  case bls12_381_g1_sig extends Multicodec("bls12_381-g1-sig", "varsig", Array[Byte](0xd0.toByte, 0xea.toByte), DRAFT)
  // G2 signature for BLS12-381 "")
  case bls12_381_g2_sig extends Multicodec("bls12_381-g2-sig", "varsig", Array[Byte](0xd0.toByte, 0xeb.toByte), DRAFT)
  // Edwards-Curve Digital Signature Algorithm "")
  case eddsa extends Multicodec("eddsa", "varsig", Array[Byte](0xd0.toByte, 0xed.toByte), DRAFT)
  // EIP-191 Ethereum Signed Data Standard "")
  case eip_191 extends Multicodec("eip-191", "varsig", Array[Byte](0xd1.toByte, 0x91.toByte), DRAFT)
  // JSON object containing only the required members of a JWK (RFC 7518 and RFC 7517) representing the public key. Serialisation based on JCS (RFC 8785) "")
  case jwk_jcs_pub extends Multicodec("jwk_jcs-pub", "key", Array[Byte](0xeb.toByte, 0x51.toByte), DRAFT)
  // Filecoin piece or sector data commitment merkle node/root (CommP & CommD) "")
  case fil_commitment_unsealed
      extends Multicodec("fil-commitment-unsealed", "filecoin", Array[Byte](0xf1.toByte, 0x01.toByte), PERMANENT)
  // Filecoin sector data commitment merkle node/root - sealed and replicated (CommR) "")
  case fil_commitment_sealed
      extends Multicodec("fil-commitment-sealed", "filecoin", Array[Byte](0xf1.toByte, 0x02.toByte), PERMANENT)
  case plaintextv2
      extends Multicodec("plaintextv2", "multiaddr", Array[Byte](0x70.toByte, 0x6c.toByte, 0x61.toByte), DRAFT)
  // Holochain v0 address    + 8 R-S (63 x Base-32) "")
  case holochain_adr_v0
      extends Multicodec("holochain-adr-v0", "holochain", Array[Byte](0x80.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Holochain v1 address    + 8 R-S (63 x Base-32) "")
  case holochain_adr_v1
      extends Multicodec("holochain-adr-v1", "holochain", Array[Byte](0x81.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Holochain v0 public key + 8 R-S (63 x Base-32) "")
  case holochain_key_v0
      extends Multicodec("holochain-key-v0", "holochain", Array[Byte](0x94.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Holochain v1 public key + 8 R-S (63 x Base-32) "")
  case holochain_key_v1
      extends Multicodec("holochain-key-v1", "holochain", Array[Byte](0x95.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Holochain v0 signature  + 8 R-S (63 x Base-32) "")
  case holochain_sig_v0
      extends Multicodec("holochain-sig-v0", "holochain", Array[Byte](0xa2.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Holochain v1 signature  + 8 R-S (63 x Base-32) "")
  case holochain_sig_v1
      extends Multicodec("holochain-sig-v1", "holochain", Array[Byte](0xa3.toByte, 0x71.toByte, 0x24.toByte), DRAFT)
  // Skynet Namespace "")
  case skynet_ns extends Multicodec("skynet-ns", "namespace", Array[Byte](0xb1.toByte, 0x99.toByte, 0x10.toByte), DRAFT)
  // Arweave Namespace "")
  case arweave_ns
      extends Multicodec("arweave-ns", "namespace", Array[Byte](0xb2.toByte, 0x99.toByte, 0x10.toByte), DRAFT)
  // Subspace Network Namespace "")
  case subspace_ns
      extends Multicodec("subspace-ns", "namespace", Array[Byte](0xb3.toByte, 0x99.toByte, 0x10.toByte), DRAFT)
  // Kumandra Network Namespace "")
  case kumandra_ns
      extends Multicodec("kumandra-ns", "namespace", Array[Byte](0xb4.toByte, 0x99.toByte, 0x10.toByte), DRAFT)
  // ES256 Signature Algorithm "")
  case es256 extends Multicodec("es256", "varsig", Array[Byte](0xd0.toByte, 0x12.toByte, 0x00.toByte), DRAFT)
  // ES384 Signature Algorithm "")
  case es284 extends Multicodec("es284", "varsig", Array[Byte](0xd0.toByte, 0x12.toByte, 0x01.toByte), DRAFT)
  // ES512 Signature Algorithm "")
  case es512 extends Multicodec("es512", "varsig", Array[Byte](0xd0.toByte, 0x12.toByte, 0x02.toByte), DRAFT)
  // RS256 Signature Algorithm "")
  case rs256 extends Multicodec("rs256", "varsig", Array[Byte](0xd0.toByte, 0x12.toByte, 0x05.toByte), DRAFT)
  // ES256K (secp256k1) Signature as Multisig "")
  case es256k_msig
      extends Multicodec("es256k-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x00.toByte), DRAFT)
  // G1 signature for BLS-12381-G2 as Multisig "")
  case bls12_381_g1_msig
      extends Multicodec("bls12_381-g1-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x01.toByte), DRAFT)
  // G2 signature for BLS-12381-G1 as Multisig "")
  case bls12_381_g2_msig
      extends Multicodec("bls12_381-g2-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x02.toByte), DRAFT)
  // Edwards-Curve Digital Signature as Multisig "")
  case eddsa_msig
      extends Multicodec("eddsa-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x03.toByte), DRAFT)
  // G1 threshold signature share for BLS-12381-G2 as Multisig "")
  case bls12_381_g1_share_msig
      extends Multicodec(
        "bls12_381-g1-share-msig",
        "multisig",
        Array[Byte](0xd0.toByte, 0x13.toByte, 0x04.toByte),
        DRAFT
      )
  // G2 threshold signature share for BLS-12381-G1 as Multisig "")
  case bls12_381_g2_share_msig
      extends Multicodec(
        "bls12_381-g2-share-msig",
        "multisig",
        Array[Byte](0xd0.toByte, 0x13.toByte, 0x05.toByte),
        DRAFT
      )
  // Lamport signature as Multisig "")
  case lamport_msig
      extends Multicodec("lamport-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x06.toByte), DRAFT)
  // Lamport threshold signature share as Multisig "")
  case lamport_share_msig
      extends Multicodec("lamport-share-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x07.toByte), DRAFT)
  // ECDSA P-256 Signature as Multisig "")
  case es256_msig
      extends Multicodec("es256-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x08.toByte), DRAFT)
  // ECDSA P-384 Signature as Multisig "")
  case es384_msig
      extends Multicodec("es384-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x09.toByte), DRAFT)
  // ECDSA P-521 Signature as Multisig "")
  case es521_msig
      extends Multicodec("es521-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x0a.toByte), DRAFT)
  // RS256 Signature as Multisig "")
  case rs256_msig
      extends Multicodec("rs256-msig", "multisig", Array[Byte](0xd0.toByte, 0x13.toByte, 0x0b.toByte), DRAFT)
  // SCION Internet architecture "")
  case scion extends Multicodec("scion", "multiaddr", Array[Byte](0xd0.toByte, 0x20.toByte, 0x00.toByte), DRAFT)
}
