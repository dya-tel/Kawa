package sibwaf.kawa.snippets

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodAnalyzerTestBase
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.extractVariables
import sibwaf.kawa.getElementsOf
import sibwaf.kawa.parseMethod
import spoon.reflect.code.CtIf
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import kotlin.test.Test

@Suppress("ClassName")
class Mage_Spell_copySpell : MethodAnalyzerTestBase() {

    private companion object {
        val TEXT = """
        public Spell copySpell(UUID newController) {
            Spell spellCopy = new Spell(this.card, this.ability.copySpell(), this.controllerId, this.fromZone);
            boolean firstDone = false;
            for (SpellAbility spellAbility : this.getSpellAbilities()) {
                if (!firstDone) {
                    firstDone = true;
                    continue;
                }
                SpellAbility newAbility = spellAbility.copy(); // e.g. spliced spell
                newAbility.newId();
                spellCopy.addSpellAbility(newAbility);
            }
            spellCopy.setCopy(true, this);
            spellCopy.setControllerId(newController);
            return spellCopy;
        }
        """.trimIndent()
    }

    private val method by lazy { parseMethod(TEXT) }

    @Test fun `Test '!firstDone' is not a constant condition`() {
        val firstDone = method.extractVariables().getValue("firstDone")
        val condition = method.getElementsOf<CtIf>().first().condition

        val flow = runBlocking { analyze(method) }
        val frame = flow.frames.getValue(condition)

        expectThat(frame.getConstraint(firstDone))
            .isA<BooleanConstraint>()
            .and {
                get { isTrue }.isFalse()
                get { isFalse }.isFalse()
            }
    }
}