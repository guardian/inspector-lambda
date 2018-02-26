package com.gu.inspectorlambda.chiefinspector

import com.gu.inspectorlambda.model.TagCombo
import org.scalatest.{FreeSpec, Matchers}

class ChiefInspectorTest extends FreeSpec with Matchers {

  "tag" - {

    "construct fully populated tag correctly" in {
      val tc = new TagCombo(Some("a"), Some("b"), Some("c"))
      ChiefInspector.constructNewTag(tc) should startWith ("a-b-c")
    }

    "construct tag with no stack correctly" in {
      val tc = new TagCombo(None, Some("b"), Some("c"))
      ChiefInspector.constructNewTag(tc) should startWith ("None-b-c")
    }

    "construct tag with no app correctly" in {
      val tc = new TagCombo(Some("a"), None, Some("c"))
      ChiefInspector.constructNewTag(tc) should startWith ("a-None-c")
    }

    "construct tag with no stage correctly" in {
      val tc = new TagCombo(Some("a"), Some("b"), None)
      ChiefInspector.constructNewTag(tc) should startWith ("a-b-None")
    }
  }
}

