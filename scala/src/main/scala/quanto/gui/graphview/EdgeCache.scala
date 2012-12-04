package quanto.gui.graphview

import quanto.data._
import quanto.gui._
import java.awt.geom.{PathIterator, CubicCurve2D, Line2D, Path2D}
import math._

case class ECache(path: Path2D.Double, lines: List[Line2D.Double])

class EdgeCache(graph: Graph[Unit,VData,Unit,Unit], trans: Transformer)
extends Iterable[(EName,ECache)] {
  private val cache = collection.mutable.Map[EName, ECache]()
  import GraphView._

  private def contactPoint(vd: VData, angle: Double): (Double,Double) = {
    vd match {
      case NodeV(c) => (c._1 + NodeRadius * cos(angle), c._2 + NodeRadius * sin(angle))
      case WireV(c) => {
        val chop = 0.707 * WireRadius
        var rad = 0d

        if (abs(WireRadius * cos(angle)) > chop) {
          rad = chop / cos(angle)
        } else if (abs(WireRadius * cos(angle)) > chop) {
          rad = chop / sin(angle)
        } else {
          rad = WireRadius
        }

        (c._1 + rad * cos(angle), c._2 + rad * sin(angle))
      }
    }
  }

  def compute() {
    for ((v1,sd) <- graph.verts; (v2,td) <- graph.verts if v2 > v1) {
      val edges = graph.source.inv(v1) intersect graph.target.inv(v2)
      val rEdges = graph.target.inv(v1) intersect graph.source.inv(v2)
      val numEdges = edges.size + rEdges.size

      if (numEdges != 0) {
        val inc = Pi * (0.666 / (numEdges + 1))
        val angle = atan2(td.coord._2 - sd.coord._2, td.coord._1 - sd.coord._1)
        var i = 1

        // first do reverse edges, then do edges


        for (e <- rEdges.iterator ++ edges.iterator if cache.get(e) == None) {
          val outAngle = angle - (0.333 * Pi) + (inc * i)
          val inAngle = angle + (1.333 * Pi) - (inc * i)
          val sp = contactPoint(sd, outAngle)
          val tp = contactPoint(td, inAngle)

          val (dx,dy) = (tp._1 - sp._1, tp._2 - sp._2)
          val handleRad = 0.333 * sqrt(dx*dx + dy*dy)

          val cp1 = (sp._1 + handleRad * cos(outAngle), sp._2 + handleRad * sin(outAngle))
          val cp2 = (tp._1 + handleRad * cos(inAngle), tp._2 + handleRad * sin(inAngle))

          val (p1,p2,p3,p4) = (
            trans toScreen sp,
            trans toScreen cp1,
            trans toScreen cp2,
            trans toScreen tp
            )

          val p = new Path2D.Double()

          val bc = new CubicCurve2D.Double(p1._1, p1._2, p2._1, p2._2, p3._1, p3._2, p4._1, p4._2)
          p.append(bc, false)

          val iter = bc.getPathIterator(null, 0.2)
          val coords = Array.ofDim[Double](6)
          var prev = (0.0,0.0)
          var lines = List[Line2D.Double]()
          while (!iter.isDone) {
            lines = iter.currentSegment(coords) match {
              case PathIterator.SEG_LINETO =>
                new Line2D.Double(prev._1, prev._2, coords(0), coords(1)) :: lines
              case _ => lines
            }
            prev = (coords(0), coords(1))
            iter.next()
          }

          val (ah1, ah2, ah3) =
            (if (edges contains e) (tp._1, tp._2, inAngle) else (sp._1, sp._2, outAngle)) match {
              case (x,y,a) => (
                trans toScreen ((x + ArrowheadLength * cos(a - ArrowheadAngle),
                  y + ArrowheadLength * sin(a - ArrowheadAngle))),
                trans toScreen (x,y),
                trans toScreen ((x + ArrowheadLength * cos(a + ArrowheadAngle),
                  y + ArrowheadLength * sin(a + ArrowheadAngle)))
                )
            }

          p.moveTo(ah1._1, ah1._2)
          p.lineTo(ah2._1, ah2._2)
          p.lineTo(ah3._1, ah3._2)

          cache(e) = ECache(p, lines)

          i += 1
        }
      }
    }
  }

  def apply(en: EName) = cache(en)
  def iterator = cache.iterator
  def setDirty(en: EName) = cache -= en
}