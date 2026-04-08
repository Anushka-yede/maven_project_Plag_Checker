package com.pc.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HumanizerServiceTest {

    @Mock   GeminiClient claude;
    @InjectMocks HumanizerService svc;

    @Test
    void humanize_returnsClaudeResponse() throws IOException {
        when(claude.complete(anyString())).thenReturn("This sounds more natural.");
        String result = svc.humanize("Furthermore, it is important to note that AI is present.");
        assertEquals("This sounds more natural.", result);
        verify(claude, times(1)).complete(anyString());
    }

    @Test
    void humanize_throwsOnBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> svc.humanize(""));
        assertThrows(IllegalArgumentException.class, () -> svc.humanize("  "));
        verifyNoInteractions(claude);
    }

    @Test
    void humanize_throwsOnTooLongInput() {
        String tooLong = "x".repeat(10_001);
        assertThrows(IllegalArgumentException.class, () -> svc.humanize(tooLong));
        verifyNoInteractions(claude);
    }

    @Test
    void humanizeWithDiff_returnsOriginalAndHumanized() throws IOException {
        when(claude.complete(anyString())).thenReturn("Human-sounding text.");
        HumanizerService.HumanizeResult result = svc.humanizeWithDiff("AI generated text.");
        assertEquals("AI generated text.", result.original());
        assertEquals("Human-sounding text.", result.humanized());
    }
}
