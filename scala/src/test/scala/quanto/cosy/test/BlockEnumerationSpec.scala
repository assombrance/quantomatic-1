package quanto.cosy.test

import quanto.cosy._
import org.scalatest.FlatSpec

/**
  * Created by hector on 28/06/17.
  */

class BlockEnumerationSpec extends FlatSpec {
  behavior of "Block Enumeration"

  it should "build a small ZW row" in {
    var rowsAllowed = BlockRowMaker(1, allowedBlocks = BlockRowMaker.ZW)
    println(rowsAllowed)
  }

  it should "build biggerZW rows" in {
    var rowsAllowed = BlockRowMaker(2, allowedBlocks = BlockRowMaker.ZW)
    println(rowsAllowed)
    assert(rowsAllowed.length == 11 * 11 + 11)
  }

  it should "stack rows" in {
    var rowsAllowed = BlockRowMaker(1, allowedBlocks = BlockRowMaker.ZW)
    var stacks = BlockStackMaker(2, rowsAllowed)
    println(stacks)
  }

  it should "limit wires" in {
    var rowsAllowed = BlockRowMaker(2, maxInOut = 2, allowedBlocks = BlockRowMaker.ZW)
    var stacks = BlockStackMaker(2, rowsAllowed)
    println(stacks)
    assert(stacks.forall(s => (s.inputs <= 2) && (s.outputs <= 2)))
  }

  it should "compute tensors" in {
    var rowsAllowed = BlockRowMaker(2, allowedBlocks = BlockRowMaker.ZW)
    var stacks = BlockStackMaker(2, rowsAllowed)
    for (elem <- stacks) {
      println("---\n" + elem.toString + " = \n" + elem.tensor)
    }
  }

  it should "compute cup x id" in {
    var allowedBlocks =  List(
      // BOTTOM TO TOP!
      Block(1, 1, " 1 ", Tensor.idWires(1)),
      Block(0, 2, "cup", new Tensor(Array(Array(1, 0, 0, 1))).transpose)
    )
    var rowsAllowed = BlockRowMaker(2, allowedBlocks= allowedBlocks)
    assert(rowsAllowed.filter(r => r.toString == "cup x  1 ").head.tensor
    == Tensor(Array(Array(1,0,0,0,0,0,1,0),Array(0,1,0,0,0,0,0,1))).transpose)
  }

  it should "find wire identities" in {
    var rowsAllowed = BlockRowMaker(1, allowedBlocks = List(
      Block(1, 1, " 1 ", Tensor.idWires(1)),
      Block(1, 1, " w ", new Tensor(Array(Array(1, 0), Array(0, -1)))),
      Block(1, 1, " b ", new Tensor(Array(Array(0, 1), Array(1, 0))))
    ))
    var stacks = BlockStackMaker(2, rowsAllowed)
    var s11 = stacks.filter(s => s.inputs == 1 && s.outputs == 1 && s.tensor.isRoughly(Tensor.idWires(1)))
    println(s11)
  }

  behavior of "blocks and JSON"

  it should "make blocks into JSON" in {
    var b = new Block(1, 1, " w ", new Tensor(Array(Array(1, 0), Array(0, -1))))
    var js1 = b.toJson
    var b2 = Block.fromJson(js1)
    assert(b2.inputs == b.inputs)
    assert(b2.tensor == b.tensor)
    println(b2)
  }

  it should "make rows into JSON" in {
    var r = new BlockRow(List(new Block(1, 1, " w ", new Tensor(Array(Array(1, 0), Array(0, -1))))))
    var js1 = r.toJson
    var r2 = BlockRow.fromJson(js1)
    assert(r2.inputs == r.inputs)
    assert(r2.tensor == r.tensor)
    println(r2)
  }

  it should "make stacks into JSON" in {
    var s = new BlockStack(
      List(new BlockRow(List(new Block(1, 1, " w ", new Tensor(Array(Array(1, 0), Array(0, -1)))))))
    )
    var js1 = s.toJson
    var s2 = BlockStack.fromJson(js1)
    assert(s2.inputs == s.inputs)
    assert(s2.tensor == s.tensor)
    println(s2)
  }
}