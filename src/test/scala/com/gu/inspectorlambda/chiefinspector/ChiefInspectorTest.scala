package com.gu.inspectorlambda.chiefinspector

import com.gu.inspectorlambda.model.{SimpleInstance, TagCombo}
import org.scalatest.{FreeSpec, Matchers}

class ChiefInspectorTest extends FreeSpec with Matchers {

  "tag construction" - {

    "construct fully populated tag correctly" in {
      val tc = new TagCombo(Some("a"), Some("b"), Some("c"))
      ChiefInspector.constructName(tc) shouldBe ("AWSInspection--a--b--c")
    }

    "construct tag with no stack correctly" in {
      val tc = new TagCombo(None, Some("b"), Some("c"))
      ChiefInspector.constructName(tc) shouldBe ("AWSInspection--None--b--c")
    }

    "construct tag with no app correctly" in {
      val tc = new TagCombo(Some("a"), None, Some("c"))
      ChiefInspector.constructName(tc) shouldBe ("AWSInspection--a--None--c")
    }

    "construct tag with no stage correctly" in {
      val tc = new TagCombo(Some("a"), Some("b"), None)
      ChiefInspector.constructName(tc) shouldBe ("AWSInspection--a--b--None")
    }
  }

  "find instances" - {
    val instance1 = SimpleInstance("instance1", Map("Stack" -> "stack1", "App" -> "app1", "Stage" -> "stage1"))
    val instance2 = SimpleInstance("instance2", Map(                     "App" -> "app2", "Stage" -> "stage2"))
    val instance3 = SimpleInstance("instance3", Map("Stack" -> "stack3", "App" -> "app3", "Stage" -> "stage3"))
    val instance4 = SimpleInstance("instance4", Map("Stack" -> "stack4",                  "Stage" -> "stage4"))
    val instance5 = SimpleInstance("instance5", Map("Stack" -> "stack5", "App" -> "app5", "Stage" -> "stage5"))
    val instance6 = SimpleInstance("instance6", Map("Stack" -> "stack6", "App" -> "app6"                     ))
    val instances = Set(instance1, instance2, instance3, instance4, instance5, instance6)

    "Stack 1 when Stack tag present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "Stack", Some("stack1")) shouldBe Set(instance1)
    }

    "Stack 2 when Stack tag not present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "Stack", None) shouldBe Set(instance2)
    }

    "Stack 3 when App tag present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "App", Some("app3")) shouldBe Set(instance3)
    }

    "Stack 4 when App tag not present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "App", None) shouldBe Set(instance4)
    }

    "Stack 5 when Stage tag present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "Stage", Some("stage5")) shouldBe Set(instance5)
    }

    "Stack 6 when Stage tag not present" in {
      ChiefInspector.getInstancesWithMatchingTag(instances, "Stage", None) shouldBe Set(instance6)
    }

    "Stack 1 when all tags present" in {
      ChiefInspector.getInstancesWithMatchingTags(instances, TagCombo(Some("stack1"), Some("app1"), Some("stage1"))) shouldBe Set("instance1")
    }
  }

  "tag combo construction" - {
    val instance1 = SimpleInstance("instance1", Map("Stack" -> "stack1", "App" -> "app1", "Stage" -> "stage1"))
    val instance2 = SimpleInstance("instance2", Map("STACK" -> "stack2", "App" -> "app2", "Stage" -> "stage2"))
    val instance3 = SimpleInstance("instance3", Map("Stack" -> "stack3", "App" -> "app3", "Stage" -> "stage3"))
    val instance4 = SimpleInstance("instance4", Map("Stack" -> "stack4", "APP" -> "app4", "Stage" -> "stage4"))
    val instance5 = SimpleInstance("instance5", Map("Stack" -> "stack5", "App" -> "app5", "Stage" -> "stage5"))
    val instance6 = SimpleInstance("instance6", Map("Stack" -> "stack6", "App" -> "app6", "STAGE" -> "stage6"))

    "getTagCombo when all present" in {
      ChiefInspector.getTagCombos(Set(instance1)) shouldBe Set(
        TagCombo(Some("stack1"), Some("app1"), Some("stage1"))
      )
    }

    "getTagCombo when stack missing" in {
      ChiefInspector.getTagCombos(Set(instance2)) shouldBe Set(
        TagCombo(None, Some("app2"), Some("stage2"))
      )
    }

    "getTagCombo when app missing" in {
      ChiefInspector.getTagCombos(Set(instance4)) shouldBe Set(
        TagCombo(Some("stack4"), None, Some("stage4"))
      )
    }

    "getTagCombo when stage missing" in {
      ChiefInspector.getTagCombos(Set(instance6)) shouldBe Set(
        TagCombo(Some("stack6"), Some("app6"), None)
      )
    }
  }
}

