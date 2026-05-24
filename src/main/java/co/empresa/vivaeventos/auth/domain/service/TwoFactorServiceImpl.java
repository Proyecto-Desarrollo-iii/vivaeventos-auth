package co.empresa.vivaeventos.auth.domain.service;

import co.empresa.vivaeventos.auth.domain.exception.CredencialesInvalidasException;
import co.empresa.vivaeventos.auth.domain.exception.TwoFactorInvalidException;
import co.empresa.vivaeventos.auth.domain.exception.UsuarioNoEncontradoException;
import co.empresa.vivaeventos.auth.domain.model.Dto.TwoFactorSetupResponse;
import co.empresa.vivaeventos.auth.domain.model.TwoFactorCode;
import co.empresa.vivaeventos.auth.domain.model.Usuario;
import co.empresa.vivaeventos.auth.domain.repository.ITwoFactorCodeRepository;
import co.empresa.vivaeventos.auth.domain.repository.IUsuarioRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class TwoFactorServiceImpl implements ITwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IUsuarioRepository usuarioRepository;
    private final ITwoFactorCodeRepository twoFactorCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorServiceImpl(IUsuarioRepository usuarioRepository, ITwoFactorCodeRepository twoFactorCodeRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.twoFactorCodeRepository = twoFactorCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse generateSetup(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();

        String secret = key.getKey();
        String qrCodeUri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "VivaEventos", email, key);

        String qrCodeBase64 = generateQRCodeBase64(qrCodeUri);

        usuario.setTwoFactorSecret(secret);
        usuario.setTwoFactorEnabled(false);
        usuarioRepository.save(usuario);

        return new TwoFactorSetupResponse(secret, qrCodeUri, qrCodeBase64);
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) return false;
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        return gAuth.authorize(secret, Integer.parseInt(code));
    }

    @Override
    @Transactional
    public void enableTwoFactor(String email, String code) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        if (usuario.getTwoFactorEnabled()) {
            throw new IllegalStateException("2FA ya esta habilitado");
        }

        if (!verifyCode(usuario.getTwoFactorSecret(), code)) {
            throw new TwoFactorInvalidException();
        }

        usuario.setTwoFactorEnabled(true);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void disableTwoFactor(String email, String password, String code) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        if (!verifyCode(usuario.getTwoFactorSecret(), code)) {
            throw new TwoFactorInvalidException();
        }

        usuario.setTwoFactorSecret(null);
        usuario.setTwoFactorEnabled(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTwoFactorEnabled(String email) {
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getTwoFactorEnabled)
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateTwoFactorMethod(String email, String method) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));
        usuario.setTwoFactorMethod(method);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTwoFactorMethod(String email) {
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getTwoFactorMethod)
                .orElse("APP");
    }

    @Override
    @Transactional
    public String sendEmailCode(UUID userId, String email) {
        twoFactorCodeRepository.deleteByUserId(userId);
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        TwoFactorCode twoFactorCode = new TwoFactorCode();
        twoFactorCode.setUserId(userId);
        twoFactorCode.setCode(code);
        twoFactorCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        twoFactorCode.setUsed(false);
        twoFactorCodeRepository.save(twoFactorCode);
        log.info("Codigo 2FA para {} ({}}): {}", email, userId, code);
        return code;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyEmailCode(UUID userId, String code) {
        return twoFactorCodeRepository
                .findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(userId, code, LocalDateTime.now())
                .isPresent();
    }

    private String generateQRCodeBase64(String uri) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(uri, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error al generar codigo QR", e);
        }
    }
}
