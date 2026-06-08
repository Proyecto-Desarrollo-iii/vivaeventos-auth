package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.TwoFactorInvalidException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorSetupResponse;
import co.empresa.vivaeventos.auth.domain.model.TwoFactorCode;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ITwoFactorCodeRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;
    @Mock
    private ITwoFactorCodeRepository twoFactorCodeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;
    @Captor
    private ArgumentCaptor<TwoFactorCode> codeCaptor;

    private TwoFactorServiceImpl twoFactorService;
    private Usuario usuario;
    private static final String EMAIL = "test@email.com";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        twoFactorService = new TwoFactorServiceImpl(usuarioRepository, twoFactorCodeRepository, passwordEncoder);
        usuario = new Usuario();
        usuario.setId(USER_ID);
        usuario.setEmail(EMAIL);
        usuario.setPassword("encodedPass");
    }

    @Test
    void shouldGenerateSetup() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TwoFactorSetupResponse response = twoFactorService.generateSetup(EMAIL);

        assertNotNull(response);
        assertNotNull(response.getSecret());
        assertNotNull(response.getQrCodeUri());
        assertNotNull(response.getQrCodeBase64());
        assertTrue(response.getQrCodeUri().startsWith("otpauth://"));
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario saved = usuarioCaptor.getValue();
        assertNotNull(saved.getTwoFactorSecret());
        assertFalse(saved.getTwoFactorEnabled());
    }

    @Test
    void shouldThrowWhenGenerateSetupForUnknownEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThrows(UsuarioNoEncontradoException.class, () -> twoFactorService.generateSetup(EMAIL));
    }

    @Test
    void shouldVerifyCodeWithRealAuthenticator() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        com.warrenstrange.googleauth.GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        String code = String.valueOf(gAuth.getTotpPassword(secret));
        assertTrue(twoFactorService.verifyCode(secret, code));
    }

    @Test
    void shouldReturnFalseWhenVerifyCodeWithNullSecret() {
        assertFalse(twoFactorService.verifyCode(null, "123456"));
    }

    @Test
    void shouldReturnFalseWhenVerifyCodeWithNullCode() {
        assertFalse(twoFactorService.verifyCode("secret", null));
    }

    @Test
    void shouldReturnFalseWhenVerifyCodeWithInvalidCode() {
        assertFalse(twoFactorService.verifyCode("invalidsecret", "000000"));
    }

    @Test
    void shouldEnableTwoFactor() {
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorEnabled(false);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TwoFactorServiceImpl spyService = spy(twoFactorService);
        doReturn(true).when(spyService).verifyCode("testSecret", "123456");

        spyService.enableTwoFactor(EMAIL, "123456");

        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertTrue(usuarioCaptor.getValue().getTwoFactorEnabled());
    }

    @Test
    void shouldThrowWhenEnableTwoFactorAlreadyEnabled() {
        usuario.setTwoFactorEnabled(true);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalStateException.class, () -> twoFactorService.enableTwoFactor(EMAIL, "123456"));
    }

    @Test
    void shouldThrowWhenEnableTwoFactorWithInvalidCode() {
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorEnabled(false);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        TwoFactorServiceImpl spyService = spy(twoFactorService);
        doReturn(false).when(spyService).verifyCode("testSecret", "000000");

        assertThrows(TwoFactorInvalidException.class, () -> spyService.enableTwoFactor(EMAIL, "000000"));
    }

    @Test
    void shouldDisableTwoFactor() {
        usuario.setTwoFactorSecret("testSecret");
        usuario.setTwoFactorEnabled(true);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password", usuario.getPassword())).thenReturn(true);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TwoFactorServiceImpl spyService = spy(twoFactorService);
        doReturn(true).when(spyService).verifyCode("testSecret", "123456");

        spyService.disableTwoFactor(EMAIL, "password", "123456");

        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario saved = usuarioCaptor.getValue();
        assertNull(saved.getTwoFactorSecret());
        assertFalse(saved.getTwoFactorEnabled());
    }

    @Test
    void shouldThrowWhenDisableTwoFactorWithWrongPassword() {
        usuario.setTwoFactorSecret("testSecret");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongPassword", usuario.getPassword())).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorService.disableTwoFactor(EMAIL, "wrongPassword", "123456"));
    }

    @Test
    void shouldThrowWhenDisableTwoFactorWithInvalidCode() {
        usuario.setTwoFactorSecret("testSecret");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password", usuario.getPassword())).thenReturn(true);

        TwoFactorServiceImpl spyService = spy(twoFactorService);
        doReturn(false).when(spyService).verifyCode("testSecret", "000000");

        assertThrows(TwoFactorInvalidException.class,
                () -> spyService.disableTwoFactor(EMAIL, "password", "000000"));
    }

    @Test
    void shouldReturnTwoFactorEnabled() {
        usuario.setTwoFactorEnabled(true);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        assertTrue(twoFactorService.isTwoFactorEnabled(EMAIL));
    }

    @Test
    void shouldReturnTwoFactorDisabled() {
        usuario.setTwoFactorEnabled(false);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        assertFalse(twoFactorService.isTwoFactorEnabled(EMAIL));
    }

    @Test
    void shouldReturnFalseWhenUserNotFoundForIsEnabled() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertFalse(twoFactorService.isTwoFactorEnabled(EMAIL));
    }

    @Test
    void shouldUpdateTwoFactorMethod() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        twoFactorService.updateTwoFactorMethod(EMAIL, "EMAIL");

        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals("EMAIL", usuarioCaptor.getValue().getTwoFactorMethod());
    }

    @Test
    void shouldGetTwoFactorMethod() {
        usuario.setTwoFactorMethod("EMAIL");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        assertEquals("EMAIL", twoFactorService.getTwoFactorMethod(EMAIL));
    }

    @Test
    void shouldDefaultToAppWhenNoMethod() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        assertEquals("APP", twoFactorService.getTwoFactorMethod(EMAIL));
    }

    @Test
    void shouldSendEmailCode() {
        when(twoFactorCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String code = twoFactorService.sendEmailCode(USER_ID, EMAIL);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
        verify(twoFactorCodeRepository).deleteByUserId(USER_ID);
        verify(twoFactorCodeRepository).save(codeCaptor.capture());
        TwoFactorCode saved = codeCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(code, saved.getCode());
        assertFalse(saved.getUsed());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldVerifyEmailCode() {
        UUID userId = UUID.randomUUID();
        String code = "123456";
        TwoFactorCode twoFactorCode = new TwoFactorCode();
        twoFactorCode.setCode(code);
        twoFactorCode.setUserId(userId);

        when(twoFactorCodeRepository.findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(
                eq(userId), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(twoFactorCode));

        assertTrue(twoFactorService.verifyEmailCode(userId, code));
    }

    @Test
    void shouldReturnFalseWhenVerifyEmailCodeNotFound() {
        when(twoFactorCodeRepository.findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        assertFalse(twoFactorService.verifyEmailCode(UUID.randomUUID(), "000000"));
    }
}
