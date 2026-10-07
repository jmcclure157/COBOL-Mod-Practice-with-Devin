package com.carddemo.seed;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CopybookReaderTest {

    @Test
    void decodesPositiveOverpunchedSign() {
        assertThat(new CopybookReader("00000001940{").signedDecimal(10, 2)).isEqualByComparingTo("194.00");
        assertThat(new CopybookReader("0000005047G").signedDecimal(9, 2)).isEqualByComparingTo("504.77");
    }

    @Test
    void decodesNegativeOverpunchedSign() {
        assertThat(new CopybookReader("0000009190}").signedDecimal(9, 2)).isEqualByComparingTo("-919.00");
        assertThat(new CopybookReader("0000000001J").signedDecimal(9, 2)).isEqualByComparingTo("-0.11");
        assertThat(new CopybookReader("0000000001R").signedDecimal(9, 2)).isEqualByComparingTo("-0.19");
    }

    @Test
    void keepsImpliedDecimalScale() {
        BigDecimal value = new CopybookReader("00150{").signedDecimal(4, 2);
        assertThat(value).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    void rejectsUnknownSignCharacter() {
        assertThatThrownBy(() -> new CopybookReader("00000001940?").signedDecimal(10, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsFieldsInCopybookOrder() {
        var r = new CopybookReader("00000000001Y2014-11-20Kessler   ");
        assertThat(r.unsignedLong(11)).isEqualTo(1L);
        assertThat(r.text(1)).isEqualTo("Y");
        assertThat(r.date()).isEqualTo(LocalDate.of(2014, 11, 20));
        assertThat(r.text(10)).isEqualTo("Kessler");
    }

    @Test
    void treatsMissingTrailingBytesAsSpaces() {
        var r = new CopybookReader("AB");
        assertThat(r.text(2)).isEqualTo("AB");
        assertThat(r.text(14)).isEmpty();
    }
}
