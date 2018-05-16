package org.constellation.tx

import java.security.KeyPair

import org.constellation.Fixtures
import org.scalatest.FlatSpec
import constellation._
import org.constellation.primitives.Schema.{Gossip, TX, TXData}

class TXTest extends FlatSpec {

  import Fixtures._
  val address1 = pubKeysToAddress(Seq(tempKey.getPublic, tempKey1.getPublic))
  val address2 = pubKeyToAddress(tempKey2.getPublic)
  val dst = pubKeyToAddress(tempKey3.getPublic)

  val tx = TX(
    TXData(
      Seq(address1, address2),
      dst,
      1L,
      keyMap = Seq(0, 0, 1)
    ).multiSigned()(Seq(tempKey, tempKey1, tempKey2))
  )

  "TX validation" should "sign and validate" in {

    assert(tx.valid)

    val tx2 = TX(
      TXData(
        Seq(address1, address2),
        dst,
        1L,
        keyMap = Seq(0, 0, 1)
      ).multiSigned()(Seq(tempKey, tempKey1, tempKey5))
    )

    assert(!tx2.valid)

  }

  "TX Gossip" should "demonstrate gossip traversal to determine stack depth" in {


    val g = Gossip(tx.signed()(tempKey))
    val gg = Gossip(g.signed()(tempKey1))
    val ggg = Gossip(gg.signed()(tempKey2))

    assert(g.stackDepth == 1)
    assert(gg.stackDepth == 2)
    assert(ggg.stackDepth == 3)

    val iter3 = ggg.iter

    assert(g.iter.size == 1)
    assert(gg.iter.size == 2)
    assert(iter3.size == 3)

    assert(iter3.head == g.event)
    assert(iter3.tail.head == gg.event)
    assert(iter3.last == ggg.event)

    assert(iter3.exists(_.publicKeys.contains(tempKey.getPublic)))
    assert(iter3.flatMap{_.publicKeys}.toSet.diff(Set(tempKey, tempKey1, tempKey2).map{_.getPublic}).isEmpty)

    assert(iter3.head.data == tx)

  }

}