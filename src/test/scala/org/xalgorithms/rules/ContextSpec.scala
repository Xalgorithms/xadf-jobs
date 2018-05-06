package org.xalgorithms.rules.elements

import org.scalamock.scalatest.MockFactory
import org.scalatest._

import org.xalgorithms.rules._
import org.xalgorithms.rules.elements._

class ContextSpec extends FlatSpec with Matchers with MockFactory {
  "GlobalContext" should "retain maps" in {
    val maps = Map(
      "map0" -> Map("a" -> new StringValue("00"), "b" -> new StringValue("01")),
      "map1" -> Map("A" -> new StringValue("xx"), "B" -> new StringValue("yy")))

    val ctx = new GlobalContext(null)

    maps.foreach { case (name, m) =>
      m.keySet.foreach { k =>
        ctx.lookup_in_map(name, k) shouldEqual(null)
      }

      ctx.retain_map(name, m)
      m.keySet.foreach { k =>
        val v = ctx.lookup_in_map(name, k)
        v should not be null
        v shouldBe a [StringValue]
        v.asInstanceOf[StringValue].value shouldEqual(m(k).value)
      }
    }
  }

  it should "retain tables" in {
    val tables = Map(
      "map0" -> Seq(
        Map("a" -> new StringValue("00"), "b" -> new StringValue("01")),
        Map("a" -> new StringValue("10"), "b" -> new StringValue("11"))),
      "map1" -> Seq(
        Map("A" -> new StringValue("xx"), "B" -> new StringValue("yy")),
        Map("A" -> new StringValue("yy"), "B" -> new StringValue("zz"))))

    val ctx = new GlobalContext(null)

    tables.foreach { case (name, table) =>
      ctx.lookup_table("tables0", name) shouldEqual(null)
      ctx.lookup_table("tables1", name) shouldEqual(null)

      ctx.retain_table("tables0", name, table)
      ctx.lookup_table("tables0", name) shouldEqual(table)
      ctx.lookup_table("tables1", name) shouldEqual(null)

      ctx.retain_table("tables1", name, table)
      ctx.lookup_table("tables0", name) shouldEqual(table)
      ctx.lookup_table("tables1", name) shouldEqual(table)
    }
  }

  "RowContext" should "evaluate _local and _context map lookups" in {
    val local_row = Map("a" -> new StringValue("00"), "b" -> new StringValue("01"))
    val context_row = Map("a" -> new StringValue("10"), "b" -> new StringValue("11"))

    val rows = Map("_local" -> local_row, "_context" -> context_row)
    val ctx = new RowContext(null, local_row, context_row)

    rows.foreach { case (section, row) =>
      row.foreach { case (key, v) =>
        val cv = ctx.lookup_in_map(section, key)

        cv should not be null
        cv shouldBe a [StringValue]
        cv.asInstanceOf[StringValue].value shouldEqual(row(key).value)
      }
    }
  }

  it should "delegate to the contained Context" in {
    val ctx = mock[Context]
    val rctx = new RowContext(ctx, Map(), Map())

    val ptref = new PackagedTableReference("", "", "", "")
    (ctx.load _).expects(ptref).once
    rctx.load(ptref)

    val section = "section"
    val m = Map("a" -> new StringValue("00"), "b" -> new StringValue("01"))
    val tbl = Seq(m)
    val tbl_key = "table0"
    val map_key = "a.b.c"
    val map_val = new StringValue("map_val")

    (ctx.retain_map _).expects(section, m).once
    rctx.retain_map(section, m)

    (ctx.retain_table _).expects(section, tbl_key, tbl)
    rctx.retain_table(section, tbl_key, tbl)

    (ctx.lookup_in_map _).expects(section, map_key).returning(map_val)
    val mv = rctx.lookup_in_map(section, map_key)
    mv should not be null
    mv shouldBe a [StringValue]
    mv.asInstanceOf[StringValue].value shouldEqual(map_val.value)

    (ctx.lookup_table _).expects(section, tbl_key).returning(tbl)
    val t = rctx.lookup_table(section, tbl_key)
    t.length shouldEqual(1)
    t(0)("a") shouldBe a [StringValue]
    t(0)("a").asInstanceOf[StringValue].value shouldEqual(m("a").value)

    val rev = new Revision(Seq())
    val rev_key = "0"
    val revisions = Map(rev_key -> Seq(rev))
    (ctx.revisions _).expects().returning(revisions)

    rctx.revisions() shouldEqual(revisions)

    (ctx.add_revision _).expects(rev_key, rev)

    rctx.add_revision(rev_key, rev)
  }
}