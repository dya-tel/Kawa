package sibwaf.kawa.values

import strikt.api.expect
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import kotlin.test.Test

class ValuesTest {

    @Test fun `Test value compositing`() {
        val value1 = Value(ValueSource.NONE)
        val value2 = Value(ValueSource.NONE)
        val value3 = Value(ValueSource.NONE)

        val composite = CompositeValue(listOf(value1, value2))

        expect {
            that(composite)
                    .describedAs("composite")
                    .and {
                        get { isSameAs(value1) }.isTrue()
                        get { isSameAs(value2) }.isTrue()
                        get { isSameAs(value3) }.isFalse()
                    }

            that(value1).describedAs("value1").get { isSameAs(composite) }.isTrue()
            that(value2).describedAs("value2").get { isSameAs(composite) }.isTrue()
            that(value3).describedAs("value3").get { isSameAs(composite) }.isFalse()
        }
    }
}