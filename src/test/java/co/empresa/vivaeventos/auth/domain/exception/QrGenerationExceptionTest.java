package co.empresa.vivaeventos.auth.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class QrGenerationExceptionTest {

    @Test
    void constructorShouldSetMessageAndCause() {
        String message = "Error generating QR code";
        Throwable cause = new RuntimeException("Underlying cause");

        QrGenerationException exception = new QrGenerationException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
