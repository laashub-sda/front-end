/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.frontend.helpers.rewriting

import org.opencypher.v9_0.rewriting.ApplyRewriter
import org.opencypher.v9_0.rewriting.DisableRewriterCondition
import org.opencypher.v9_0.rewriting.EnableRewriterCondition
import org.opencypher.v9_0.rewriting.RewriterCondition
import org.opencypher.v9_0.rewriting.RewriterContract
import org.opencypher.v9_0.rewriting.RewriterStepSequencer
import org.opencypher.v9_0.rewriting.RunConditionRewriter
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class RewriterStepSequencerTest extends CypherFunSuite {

  test("if no conditions are used, what goes in is what comes out") {
    val dummyRewriter1 = Rewriter.noop
    val dummyRewriter2 = Rewriter.lift { case x: AnyRef => x }

    RewriterStepSequencer.newValidating("test")() should equal(RewriterContract(Seq(), Set()))
    RewriterStepSequencer.newValidating("test")(ApplyRewriter("1", dummyRewriter1), ApplyRewriter("2", dummyRewriter2)) should equal(RewriterContract(Seq(dummyRewriter1, dummyRewriter2), Set()))
  }

  test("Should enable conditions between rewriters and collect the post conditions at the end") {
    val dummyCond1 = RewriterCondition("a", (_: Any) => Seq("1"))
    val dummyCond2 = RewriterCondition("b", (_: Any) => Seq("2"))
    val dummyRewriter1 = Rewriter.noop
    val dummyRewriter2 = Rewriter.lift { case x: AnyRef => x }

    val sequencer = RewriterStepSequencer.newValidating("test")(
      ApplyRewriter("1", dummyRewriter1),
      EnableRewriterCondition(dummyCond1),
      ApplyRewriter("2", dummyRewriter2),
      EnableRewriterCondition(dummyCond2)
    )

    sequencer.childRewriters should equal(Seq(
      dummyRewriter1,
      RunConditionRewriter("test", Some("1"), Set(dummyCond1)),
      dummyRewriter2,
      RunConditionRewriter("test", Some("2"), Set(dummyCond1, dummyCond2))
    ))
    sequencer.postConditions should equal(Set(dummyCond1, dummyCond2))
  }

  test("Should enable/disable conditions between rewriters and collect the post conditions at the end") {
    val dummyCond1 = RewriterCondition("a", (_: Any) => Seq("1"))
    val dummyCond2 = RewriterCondition("b", (_: Any) => Seq("2"))
    val dummyRewriter1 = Rewriter.noop
    val dummyRewriter2 = Rewriter.lift { case x: AnyRef => x}
    val dummyRewriter3 = Rewriter.noop

    val sequencer = RewriterStepSequencer.newValidating("test")(
      ApplyRewriter("1", dummyRewriter1),
      EnableRewriterCondition(dummyCond1),
      ApplyRewriter("2", dummyRewriter2),
      EnableRewriterCondition(dummyCond2),
      ApplyRewriter("3", dummyRewriter3),
      DisableRewriterCondition(dummyCond2)
    )

    sequencer.childRewriters should equal(Seq(
      dummyRewriter1,
      RunConditionRewriter("test", Some("1"), Set(dummyCond1)),
      dummyRewriter2,
      RunConditionRewriter("test", Some("2"), Set(dummyCond1, dummyCond2)),
      dummyRewriter3,
      RunConditionRewriter("test", Some("3"), Set(dummyCond1))
    ))
    sequencer.postConditions should equal(Set(dummyCond1))
  }
}
