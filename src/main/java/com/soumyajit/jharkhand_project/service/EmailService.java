package com.soumyajit.jharkhand_project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    private GeoIpService geoIpService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Verify your email address - Jharkhand Bihar Updates");
            helper.setText(buildOtpEmailTemplate(otp), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    @Async
    public void sendWelcomeEmail(String to, String firstName, String lastName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Welcome to Jharkhand Bihar Updates! 🎉");
            helper.setText(buildWelcomeEmailTemplate(firstName, lastName), true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("🔒 Password Reset - Jharkhand Bihar Updates");
            helper.setText(buildPasswordResetEmailTemplate(resetToken), true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

    private String buildPasswordResetEmailTemplate(String resetToken) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Password Reset - Jharkhand Bihar Updates</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; background-color: ##f8fafc;">
            
            <div style="max-width: 600px; margin: 20px auto; background-color: ##ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);">
                
                <!-- Header -->
                <div style="background: linear-gradient(135deg, ##dc2626 0%%, ##ef4444 100%%); padding: 40px 30px; text-align: center;">
                    <div style="background: rgba(255,255,255,0.15); border-radius: 50%%; width: 80px; height: 80px; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;">
                        <span style="font-size: 36px;">🔒</span>
                    </div>
                    <h1 style="margin: 0; color: ##ffffff; font-size: 28px; font-weight: 700;">Password Reset</h1>
                    <p style="margin: 8px 0 0 0; color: ##fecaca; font-size: 16px;">Jharkhand Bihar Updates</p>
                </div>
                
                <!-- Content -->
                <div style="padding: 40px 35px;">
                    <h2 style="margin: 0 0 20px 0; color: ##1f2937; font-size: 24px; font-weight: 600;">
                        Reset Your Password
                    </h2>
                    
                    <p style="margin: 0 0 25px 0; color: ##4b5563; font-size: 16px; line-height: 1.6;">
                        We received a request to reset your password for your Jharkhand Bihar Updates account. 
                        Use the verification code below to set a new password.
                    </p>
                    
                    <!-- Security Code -->
                    <div style="background: linear-gradient(135deg, ##fef2f2 0%%, ##fee2e2 100%%); border: 2px solid ##ef4444; border-radius: 12px; padding: 30px; margin: 30px 0; text-align: center;">
                        <p style="margin: 0 0 15px 0; color: ##7f1d1d; font-size: 16px; font-weight: 600;">
                            Your Reset Code
                        </p>
                        <div style="background: ##ffffff; border-radius: 8px; padding: 20px; margin: 15px 0;">
                            <span style="font-size: 24px; font-weight: 900; color: ##dc2626; letter-spacing: 2px; font-family: 'Courier New', monospace; word-break: break-all;">
                                %s
                            </span>
                        </div>
                        <p style="margin: 15px 0 0 0; color: ##991b1b; font-size: 14px;">
                            ⏰ This code expires in <strong>5 minutes</strong>
                        </p>
                    </div>
                    
                    <!-- Instructions -->
                    <div style="background: ##f0f9ff; border-left: 4px solid ##3b82f6; padding: 20px; margin: 25px 0; border-radius: 0 6px 6px 0;">
                        <h4 style="margin: 0 0 10px 0; color: ##1e40af; font-size: 16px; font-weight: 600;">
                            📝 How to use this code:
                        </h4>
                        <ol style="margin: 0; color: ##1e3a8a; font-size: 14px; line-height: 1.6; padding-left: 20px;">
                            <li>Go to the password reset page</li>
                            <li>Enter your email address</li>
                            <li>Paste the code above</li>
                            <li>Create your new secure password</li>
                        </ol>
                    </div>
                    
                    <!-- Security Warning -->
                    <div style="background: ##fef3c7; border: 1px solid ##f59e0b; border-radius: 8px; padding: 20px; margin: 25px 0;">
                        <div style="display: flex; align-items: flex-start;">
                            <span style="color: ##d97706; font-size: 20px; margin-right: 12px;">⚠️</span>
                            <div>
                                <h4 style="margin: 0 0 8px 0; color: ##92400e; font-size: 16px; font-weight: 600;">
                                    Security Notice
                                </h4>
                                <p style="margin: 0; color: ##78350f; font-size: 14px; line-height: 1.5;">
                                    If you didn't request this password reset, please ignore this email. 
                                    Your password will remain unchanged, and your account stays secure.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Footer -->
                <div style="background: ##f9fafb; padding: 25px 35px; border-top: 1px solid ##e5e7eb; text-align: center;">
                    <p style="margin: 0 0 15px 0; color: ##6b7280; font-size: 12px;">
                        This message was sent from Jharkhand Bihar Updates security system.<br>
                        © 2026 Jharkhand Bihar Updates. All rights reserved.
                    </p>
                    <p style="margin: 0; color: ##9ca3af; font-size: 11px;">
                        <a href="##" style="color: ##3b82f6; text-decoration: none;">Contact Support</a> | 
                        <a href="##" style="color: ##3b82f6; text-decoration: none;">Security Help</a>
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(resetToken);
    }


    @Async
    public void sendPasswordResetConfirmationEmail(String to, String firstName, String lastName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("✅ Password Successfully Reset - Jharkhand Bihar Updates");
            helper.setText(buildPasswordResetConfirmationTemplate(to,firstName, lastName), true);

            mailSender.send(message);
            log.info("Password reset confirmation email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset confirmation email to: {}", to, e);
        }
    }

    private String buildPasswordResetConfirmationTemplate(String email,String firstName, String lastName) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Password Reset Successful - Jharkhand Bihar Updates</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; background-color: ##f8fafc;">
            
            <div style="max-width: 600px; margin: 20px auto; background-color: ##ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);">
                
                <!-- Header - Success Theme -->
                <div style="background: linear-gradient(135deg, ##059669 0%%, ##10b981 100%%); padding: 40px 30px; text-align: center;">
                    <div style="background: rgba(255,255,255,0.15); border-radius: 50%%; width: 80px; height: 80px; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;">
                        <span style="font-size: 36px;">✅</span>
                    </div>
                    <h1 style="margin: 0; color: ##ffffff; font-size: 28px; font-weight: 700;">Password Reset Successful</h1>
                    <p style="margin: 8px 0 0 0; color: ##d1fae5; font-size: 16px;">Jharkhand Bihar Updates</p>
                </div>
                
                <!-- Content -->
                <div style="padding: 40px 35px;">
                    
                    <!-- Personal Greeting -->
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h2 style="margin: 0 0 15px 0; color: ##1f2937; font-size: 24px; font-weight: 600;">
                            Hello %s %s! 👋
                        </h2>
                        <p style="margin: 0; color: ##4b5563; font-size: 18px; line-height: 1.6;">
                            Your password has been <strong style="color: ##059669;">successfully reset</strong>. 
                            Your account is now secure with your new password.
                        </p>
                    </div>
                    
                    <!-- Success Confirmation -->
                    <div style="background: linear-gradient(135deg, ##ecfdf5 0%%, ##d1fae5 100%%); border: 1px solid ##10b981; border-radius: 8px; padding: 25px; margin: 30px 0; text-align: center;">
                        <div style="color: ##059669; font-size: 20px; margin-bottom: 12px;">🔐</div>
                        <h3 style="margin: 0 0 10px 0; color: ##065f46; font-size: 18px; font-weight: 600;">
                            Password Change Confirmed
                        </h3>
                        <p style="margin: 0; color: ##047857; font-size: 15px; line-height: 1.5;">
                            This change was completed on <strong>%s</strong> at <strong>%s</strong>
                        </p>
                    </div>
                    
                    <!-- What's Next Section -->
                    <div style="background: ##f0f9ff; border-left: 4px solid ##3b82f6; padding: 20px; margin: 25px 0; border-radius: 0 6px 6px 0;">
                        <h4 style="margin: 0 0 12px 0; color: ##1e40af; font-size: 18px; font-weight: 600;">
                            🚀 What's Next?
                        </h4>
                        <ul style="margin: 0; color: ##1e3a8a; font-size: 15px; line-height: 1.6; padding-left: 20px;">
                            <li style="margin-bottom: 8px;">You can now log in with your new password</li>
                            <li style="margin-bottom: 8px;">All your previous sessions have been logged out for security</li>
                            <li style="margin-bottom: 8px;">Continue enjoying news, events, job, community updates from Jharkhand</li>
                        </ul>
                    </div>
                    
                    <!-- Security Tips -->
                    <div style="background: linear-gradient(135deg, ##fef3c7 0%%, ##fde68a 100%%); border: 1px solid ##f59e0b; border-radius: 8px; padding: 20px; margin: 25px 0;">
                        <div style="display: flex; align-items: flex-start;">
                            <span style="color: ##d97706; font-size: 20px; margin-right: 12px;">💡</span>
                            <div>
                                <h4 style="margin: 0 0 10px 0; color: ##92400e; font-size: 16px; font-weight: 600;">
                                    Security Tips
                                </h4>
                                <ul style="margin: 0; color: ##78350f; font-size: 14px; line-height: 1.5; padding-left: 16px;">
                                    <li style="margin-bottom: 6px;">Use a unique password that you don't use elsewhere</li>
                                    <li style="margin-bottom: 6px;">Consider using a password manager</li>
                                    <li style="margin-bottom: 6px;">Keep your login credentials confidential</li>
                                    <li>Log out from shared devices after use</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Call to Action -->
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="##" style="display: inline-block; background: linear-gradient(135deg, ##059669 0%%, ##10b981 100%%); color: ##ffffff; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: 600; font-size: 16px; margin: 5px;">
                            🏠 Go to Dashboard
                        </a>
                        <a href="##" style="display: inline-block; background: transparent; color: ##059669; text-decoration: none; padding: 14px 28px; border: 2px solid ##10b981; border-radius: 6px; font-weight: 600; font-size: 16px; margin: 5px;">
                            📰 Read Latest News
                        </a>
                    </div>
                    
                    <!-- Suspicious Activity Warning -->
                    <div style="background: linear-gradient(135deg, ##fef2f2 0%%, ##fee2e2 100%%); border: 1px solid ##f87171; border-radius: 8px; padding: 20px; margin: 30px 0;">
                        <div style="display: flex; align-items: flex-start;">
                            <span style="color: ##ef4444; font-size: 20px; margin-right: 12px;">⚠️</span>
                            <div>
                                <h4 style="margin: 0 0 8px 0; color: ##dc2626; font-size: 16px; font-weight: 600;">
                                    Didn't Reset Your Password?
                                </h4>
                                <p style="margin: 0 0 15px 0; color: ##7f1d1d; font-size: 14px; line-height: 1.5;">
                                    If you didn't initiate this password reset, your account may be compromised. 
                                    Please contact our security team immediately.
                                </p>
                                <div style="margin-top: 12px;">
                                    <a href="mailto:support@jharkhandupdates.com" style="color: ##dc2626; text-decoration: none; font-weight: 600; margin-right: 15px;">
                                        📧 support@jharkhandupdates.com
                                    </a>
                                    <a href="##" style="color: ##dc2626; text-decoration: none; font-weight: 600;">
                                        📞 +91 99054 04064
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Account Details -->
                    <div style="background: ##f8fafc; border-radius: 6px; padding: 20px; margin: 25px 0;">
                        <h4 style="margin: 0 0 12px 0; color: ##374151; font-size: 16px; font-weight: 600;">
                            📋 Account Details
                        </h4>
                        <div style="color: ##6b7280; font-size: 14px; line-height: 1.6;">
                            <p style="margin: 0 0 6px 0;"><strong>Account:</strong> %s</p>
                            <p style="margin: 0 0 6px 0;"><strong>User:</strong> %s %s</p>
                            <p style="margin: 0 0 6px 0;"><strong>Date:</strong> %s</p>
                            <p style="margin: 0;"><strong>Time:</strong> %s IST</p>
                        </div>
                    </div>
                </div>
                
                <!-- Footer -->
                <div style="background: ##f3f4f6; padding: 30px 35px; border-top: 1px solid ##e5e7eb;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h3 style="margin: 0 0 8px 0; color: ##1f2937; font-size: 18px; font-weight: 600;">Jharkhand Bihar Updates</h3>
                        <p style="margin: 0; color: ##6b7280; font-size: 14px; font-style: italic;">
                            Connecting Jharkhand and Bihar • Empowering Communities • Building Tomorrow
                        </p>
                    </div>
                    
                    <!-- Support Links -->
                    <div style="text-align: center; margin: 20px 0;">
                        <a href="##" style="color: ##3b82f6; text-decoration: none; font-weight: 500; margin: 0 10px; font-size: 14px;">Help Center</a>
                        <span style="color: ##cbd5e1;">|</span>
                        <a href="##" style="color: ##3b82f6; text-decoration: none; font-weight: 500; margin: 0 10px; font-size: 14px;">Contact Support</a>
                        <span style="color: ##cbd5e1;">|</span>
                        <a href="##" style="color: ##3b82f6; text-decoration: none; font-weight: 500; margin: 0 10px; font-size: 14px;">Security Center</a>
                    </div>
                    
                    <!-- Copyright -->
                    <div style="text-align: center; padding-top: 15px; border-top: 1px solid ##d1d5db;">
                        <p style="margin: 0 0 8px 0; color: ##4b5563; font-size: 12px;">
                            © 2026 Jharkhand Bihar Updates . All rights reserved.
                        </p>
                        <p style="margin: 0; color: ##6b7280; font-size: 11px;">
                            <a href="##" style="color: ##3b82f6; text-decoration: none;">Privacy Policy</a> | 
                            <a href="##" style="color: ##3b82f6; text-decoration: none;">Terms of Service</a> | 
                            <a href="##" style="color: ##3b82f6; text-decoration: none;">Security Policy</a>
                        </p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                firstName, lastName,
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                email,
                firstName, lastName,
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        );
    }



    private String buildWelcomeEmailTemplate(String firstName, String lastName) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Welcome to Jharkhand Bihar Updates</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f8fafc;
            color: #374151;
            line-height: 1.6;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }
        .container {
            max-width: 600px;
            margin: 20px auto;
            background-color: #ffffff;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #2563eb 0%%, #3b82f6 100%%);
            padding: 40px 20px;
            text-align: center;
            color: #ffffff;
        }
        .header h1 {
            margin: 0 0 10px 0;
            font-size: 34px;
            font-weight: 700;
            letter-spacing: -0.5px;
            text-shadow: 0 1px 3px rgba(0,0,0,0.3);
        }
        .header p {
            margin: 0;
            font-size: 16px;
            font-weight: 400;
            color: #dbeafe;
        }
        .content {
            padding: 40px 35px;
        }
        .welcome-msg {
            text-align: center;
            margin-bottom: 40px;
        }
        .welcome-msg h2 {
            font-size: 28px;
            color: #111827;
            font-weight: 600;
            margin-bottom: 12px;
        }
        .welcome-msg p {
            font-size: 18px;
            color: #4b5563;
            margin: 0 auto;
            max-width: 480px;
        }
        .badge {
            background: linear-gradient(135deg, #ecfdf5 0%%, #d1fae5 100%%);
            border: 1px solid #10b981;
            border-radius: 8px;
            padding: 22px 20px;
            margin: 30px 0;
            text-align: center;
            color: #065f46;
            font-weight: 600;
            font-size: 16px;
            box-shadow: 0 2px 8px rgba(16,185,129,0.25);
        }
        .badge-icon {
            font-size: 24px;
            color: #059669;
            margin-bottom: 8px;
        }
        .about-section {
            background: #f8fafc;
            border-left: 5px solid #3b82f6;
            border-radius: 0 8px 8px 0;
            padding: 25px 30px;
            margin: 35px 0;
            color: #374151;
            font-size: 15px;
        }
        .about-section h3 {
            color: #1e40af;
            font-size: 21px;
            font-weight: 600;
            margin-bottom: 15px;
        }
        .about-section p {
            margin-bottom: 25px;
            line-height: 1.6;
        }
        .stats {
            display: flex;
            justify-content: space-around;
            max-width: 420px;
            margin: 0 auto;
        }
        .stats div {
            text-align: center;
        }
        .stats .number {
            font-size: 26px;
            font-weight: 700;
            color: #1e40af;
        }
        .stats .label {
            font-size: 13px;
            color: #6b7280;
            margin-top: 4px;
        }
        .features-section {
            margin-top: 40px;
            text-align: center;
        }
        .features-section h3 {
            color: #1f2937;
            font-size: 24px;
            font-weight: 600;
            margin-bottom: 30px;
        }
        .feature-box {
            border-radius: 8px;
            padding: 18px 20px;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            max-width: 500px;
            margin-left: auto;
            margin-right: auto;
        }
        .feature-icon {
            border-radius: 6px;
            width: 42px;
            height: 42px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            color: white;
            margin-right: 15px;
            flex-shrink: 0;
        }
        .feature-news {
            background: #fee2e2;
            border: 1px solid #fecaca;
            color: #7f1d1d;
        }
        .feature-news .feature-icon {
            background: #ef4444;
        }
        .feature-events {
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #1e3a8a;
        }
        .feature-events .feature-icon {
            background: #3b82f6;
        }
        .feature-jobs {
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            color: #065f46;
        }
        .feature-jobs .feature-icon {
            background: #10b981;
        }
        .feature-community {
            background: #faf5ff;
            border: 1px solid #e9d5ff;
            color: #581c87;
        }
        .feature-community .feature-icon {
            background: #a855f7;
        }
        .feature-content {
            margin: 0;
            font-size: 14px;
            font-weight: 600;
        }
        .feature-title {
            margin: 0 0 5px 0;
            font-size: 16px;
        }
        .cta-buttons {
            margin-top: 40px;
            text-align: center;
        }
        .cta-button {
            display: inline-block;
            padding: 12px 28px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 15px;
            text-decoration: none;
            margin: 0 12px 12px 12px;
            transition: all 0.3s ease;
        }
        .cta-primary {
            background: linear-gradient(135deg, #2563eb 0%%, #3b82f6 100%%);
            color: #fff;
        }
        .cta-primary:hover {
            background: linear-gradient(135deg, #1e40af 0%%, #2563eb 100%%);
        }
        .cta-secondary {
            border: 2px solid #3b82f6;
            color: #2563eb;
            background: transparent;
        }
        .cta-secondary:hover {
            background: #3b82f6;
            color: #fff;
        }
        .support-section {
            background: #f8fafc;
            text-align: center;
            border-radius: 8px;
            padding: 25px 30px;
            margin: 40px 0;
            color: #6b7280;
            font-size: 15px;
        }
        .support-section h4 {
            color: #374151;
            font-size: 18px;
            font-weight: 600;
            margin-bottom: 15px;
        }
        .support-section a {
            color: #3b82f6;
            text-decoration: none;
            font-weight: 500;
            margin: 0 10px;
        }
        .support-section a:hover {
            text-decoration: underline;
        }
        .social-media {
            text-align: center;
            margin: 30px 0;
        }
        .social-media p {
            font-weight: 500;
            color: #6b7280;
            font-size: 16px;
            margin-bottom: 15px;
        }
        .social-icons {
            display: flex;
            justify-content: center;
            gap: 15px;
        }
        .social-icons a {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 38px;
            height: 38px;
            border-radius: 7px;
            text-decoration: none;
            transition: all 0.3s ease;
        }
        .social-icons a svg {
            width: 18px;
            height: 18px;
            fill: white;
        }
        
        /* Footer */
        .footer {
            background: #f3f4f6;
            padding: 30px 35px;
            border-top: 1px solid #e5e7eb;
            font-size: 14px;
            color: #6b7280;
        }
        .footer-content {
            display: flex;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 25px;
        }
        .footer-section {
            flex: 1;
            min-width: 250px;
        }
        .footer-section h4 {
            color: #1f2937;
            font-weight: 600;
            margin-bottom: 10px;
        }
        .footer-section p, .footer-section a {
            color: #6b7280;
            font-size: 14px;
            text-decoration: none;
        }
        .footer-section a:hover {
            text-decoration: underline;
        }
        .footer-social-icons {
            display: flex;
            gap: 8px;
            margin-top: 12px;
        }
        .footer-social-icons a {
            display: inline-block;
            width: 32px;
            height: 32px;
            border-radius: 4px;
            color: white;
            text-align: center;
            line-height: 32px;
            font-size: 12px;
            background-color: #000000;
        }
        .footer-copyright {
            border-top: 1px solid #d1d5db;
            padding-top: 15px;
            text-align: center;
            font-size: 12px;
            color: #4b5563;
            margin-top: 25px;
        }
        .footer-company {
            text-align: center;
            padding-top: 20px;
            border-top: 1px solid #d1d5db;
            color: #6b7280;
            font-style: italic;
            font-size: 14px;
            margin-top: 25px;
        }
        @media (max-width: 620px) {
            .container {
                margin: 10px 20px;
            }
            .content {
                padding: 30px 20px;
            }
            .footer-content {
                flex-direction: column;
            }
            .footer-section {
                min-width: 100%%;
                margin-bottom: 20px;
            }
            .stats {
                flex-direction: column;
                gap: 15px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Jharkhand Bihar Updates</h1>
            <p>Your trusted digital gateway</p>
        </div>

        <div class="content">
            <div class="welcome-msg">
                <h2>Hello %s %s! 👋</h2>
                <p>Thank you for joining our growing community of <strong style="color: #2563eb;">50,000+</strong> readers! We're thrilled to have you as part of the Jharkhand Bihar Updates family.</p>
            </div>

            <div class="badge">
                <div class="badge-icon">✅</div>
                Account Successfully Verified & Activated
            </div>

            <div class="about-section">
                <h3>🌟 About Jharkhand Bihar Updates</h3>
                <p>Since 2018, we've been Jharkhand's and Bihar's premier digital news platform, serving authentic local news, events, job opportunities, and community stories across all 24 districts. Our mission is to keep you connected with what matters most in your region.</p>
                <div class="stats">
                    <div>
                        <div class="number">24</div>
                        <div class="label">Districts Covered</div>
                    </div>
                    <div>
                        <div class="number">50K+</div>
                        <div class="label">Active Readers</div>
                    </div>
                    <div>
                        <div class="number">1000+</div>
                        <div class="label">Jobs Posted</div>
                    </div>
                </div>
            </div>

            <div class="features-section">
                <h3>🚀 Explore What's Waiting For You</h3>

                <div class="feature-box feature-news">
                    <div class="feature-icon">📰</div>
                    <div>
                        <h4 class="feature-title">Breaking News & Updates</h4>
                        <p class="feature-content">Real-time news from all districts</p>
                    </div>
                </div>

                <div class="feature-box feature-events">
                    <div class="feature-icon">🎪</div>
                    <div>
                        <h4 class="feature-title">Local Events & Festivals</h4>
                        <p class="feature-content">Never miss cultural celebrations</p>
                    </div>
                </div>

                <div class="feature-box feature-jobs">
                    <div class="feature-icon">💼</div>
                    <div>
                        <h4 class="feature-title">Career Opportunities</h4>
                        <p class="feature-content">Government & private sector jobs</p>
                    </div>
                </div>

                <div class="feature-box feature-community">
                    <div class="feature-icon">💬</div>
                    <div>
                        <h4 class="feature-title">Community Hub</h4>
                        <p class="feature-content">Connect, share, and engage with locals</p>
                    </div>
                </div>
            </div>

            <div class="cta-buttons">
                <a href="https://www.jharkhandbiharupdates.com" class="cta-button cta-secondary">🌐 Visit Website</a>
            </div>

            <div class="support-section">
                <h4>🤝 Need Help? We're Here For You!</h4>
                <p>Our dedicated support team is available 24/7 to assist you. Whether you have questions about features, need technical help, or want to share feedback - we're just one click away!</p>
                <div>
                    <a href="mailto:support@jharkhandupdates.com">📧 Email Support</a>
                    <span style="color:#cbd5e1; margin: 0 5px;">|</span>
                    <a href="#">💬 Live Chat</a>
                    <span style="color:#cbd5e1; margin: 0 5px;">|</span>
                    <a href="9905404064">📞 Call Us</a>
                </div>
            </div>

            <div class="social-media">
                <p>Follow us for daily updates:</p>
                <div class="social-icons">
                                    <a href="https://x.com/JhUpdate" class="twitter" aria-label="Twitter">
                                        <img src="https://cdn-icons-png.flaticon.com/512/733/733579.png" alt="Twitter" width="20" height="20">
                                    </a>
                                    <a href="https://www.facebook.com/JhUpdate" class="facebook" aria-label="Facebook">
                                        <img src="https://cdn-icons-png.flaticon.com/512/733/733547.png" alt="Facebook" width="20" height="20">
                                    </a>
                                    <a href="https://www.instagram.com/jhupdate/" class="instagram" aria-label="Instagram">
                                        <img src="https://cdn-icons-png.flaticon.com/512/733/733558.png" alt="Instagram" width="20" height="20">
                                    </a>
                                    <a href="https://whatsapp.com/channel/0029VafHItD1SWsuSX7xQA3P" class="whatsapp" aria-label="WhatsApp">
                                        <img src="https://cdn-icons-png.flaticon.com/512/733/733585.png" alt="WhatsApp" width="20" height="20">
                                    </a>
                                </div>
                
            </div>
        </div>

        <div class="footer">
            <div class="footer-content">
                <div class="footer-section">
                    <h4>Follow us:</h4>
                    <p>
                        Stay connected with Jharkhand Bihar Updates for the latest updates, breaking news, and community stories from across all 24 districts.
                    </p>
                    <div class="social-icons">
                                              <a href="https://x.com/JhUpdate" class="twitter" aria-label="Twitter">
                                                  <img src="https://cdn-icons-png.flaticon.com/512/733/733579.png" alt="Twitter" width="20" height="20">
                                              </a>
                                              <a href="https://www.facebook.com/JhUpdate" class="facebook" aria-label="Facebook">
                                                  <img src="https://cdn-icons-png.flaticon.com/512/733/733547.png" alt="Facebook" width="20" height="20">
                                              </a>
                                              <a href="https://www.instagram.com/jhupdate/" class="instagram" aria-label="Instagram">
                                                  <img src="https://cdn-icons-png.flaticon.com/512/733/733558.png" alt="Instagram" width="20" height="20">
                                              </a>
                                              <a href="https://whatsapp.com/channel/0029VafHItD1SWsuSX7xQA3P" class="whatsapp" aria-label="WhatsApp">
                                                  <img src="https://cdn-icons-png.flaticon.com/512/733/733585.png" alt="WhatsApp" width="20" height="20">
                                              </a>
                                          </div>
                </div>
                <div class="footer-section">
                    <h4>Contact us:</h4>
                    <p>📧 <a href="mailto:support@jharkhandupdates.com">support@jharkhandupdates.com</a></p>
                    <p>📞 +91 99054 04064</p>
                </div>
            </div>
            <div class="footer-company">
                Jharkhand Bihar Updates<br>
                Connecting Jharkhand and Bihar • Empowering Communities • Building Tomorrow
            </div>
            <div class="footer-copyright">
                &copy; 2026 Jharkhand Bihar Updates. All rights reserved.<br>
                <a href="https://jharkhandupdates.com/privacy-policy">Privacy Policy</a> | 
                <a href="https://jharkhandupdates.com/terms-conditions">Terms of Service</a> | 
                <a href="#">Unsubscribe</a>
            </div>
        </div>
    </div>
</body>
</html>
    """.formatted(firstName, lastName);
    }










    private String buildOtpEmailTemplate(String otp) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Verification - Jharkhand & Bihar Updates</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                    
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); padding: 40px 20px; text-align: center;">
                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: bold; letter-spacing: 1px;">
                            Jharkhand & Bihar Updates
                        </h1>
                        <p style="margin: 8px 0 0 0; color: #e0e7ff; font-size: 14px;">
                            Your trusted news source for Jharkhand
                        </p>
                    </div>
                    
                    <!-- Content -->
                    <div style="padding: 40px 30px;">
                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600;">
                            Verify your email address
                        </h2>
                        
                        <p style="margin: 0 0 25px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                            Thanks for joining Jharkhand & Bihar Updates! We want to make sure it's really you. 
                            Please enter the following verification code when prompted. If you don't want to create an account, you can ignore this message.
                        </p>
                        
                        <!-- OTP Section -->
                        <div style="text-align: center; margin: 35px 0;">
                            <p style="margin: 0 0 15px 0; color: #374151; font-size: 16px; font-weight: 600;">
                                Verification code
                            </p>
                            <div style="background-color: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 25px; margin: 20px 0;">
                                <span style="font-size: 36px; font-weight: bold; color: #1e40af; letter-spacing: 4px; font-family: 'Courier New', monospace;">
                                    """ + otp + """
                                </span>
                            </div>
                            <p style="margin: 15px 0 0 0; color: #6b7280; font-size: 14px;">
                                (This code is valid for 5 minutes)
                            </p>
                        </div>
                        
                        <!-- Security Notice -->
                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 16px; margin: 30px 0; border-radius: 0 6px 6px 0;">
                            <p style="margin: 0; color: #92400e; font-size: 14px; line-height: 1.5;">
                                <strong>Security Notice:</strong> Jharkhand & Bihar Updates will never email you and ask you to disclose or verify your password, credit card, or banking account number.
                            </p>
                        </div>
                    </div>
                    
                    <!-- Footer -->
                    <div style="background-color: #f9fafb; padding: 25px 30px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 12px; line-height: 1.5;">
                        <p style="margin: 0 0 8px 0;">
                            This message was produced and distributed by Jharkhand & Bihar Updates. 
                            © 2025, Mehraki Pvt Ltd. All rights reserved. Jharkhand & Bihar Updates is a registered trademark of Jharkhand & Bihar Updates Inc.
                        </p>
                        <p style="margin: 8px 0 0 0;">
                            View our <a href="#" style="color: #3b82f6; text-decoration: none;">privacy policy</a> | 
                            <a href="#" style="color: #3b82f6; text-decoration: none;">Contact Support</a>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    @Async("taskExecutor") // Specifies which executor to use
    public void sendLoginAlertEmail(String to, String device, String ip, String dateTime) {
        try {
            String location = geoIpService.getLocation(ip);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("New Login Alert - Jharkhand Bihar Updates");
            helper.setText(buildLoginAlertTemplate(device, location, dateTime), true);

            mailSender.send(message);
            log.info("Login alert sent to {} for login from {}", to, location);
        } catch (MessagingException e) {
            log.error("Failed to send login alert email to {}", to, e);
        }
    }

    private String buildLoginAlertTemplate(String device, String location, String dateTime) {
        return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Login Alert - Jharkhand Bihar Updates</title>
    </head>
    <body style="margin:0; padding:0; background:#f5f6fa; font-family:Segoe UI, Arial, sans-serif; color:#232323;">
        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f5f6fa; padding: 0;">
            <tr>
                <td align="center">
                <table role="presentation" width="480" cellspacing="0" cellpadding="0" border="0" style="background:#fff; margin:30px 0; box-shadow:0 1px 8px #0001; border-radius:10px;">
                    <tr>
                        <td align="center" style="padding:32px 24px 18px 24px;">
                            <h2 style="margin:0; color:#222; font-size:1.7em; letter-spacing:-0.5px"><b>Jharkhand News</b></h2>
                            <p style="color:#666; margin:9px 0 0 0; font-size:17px; font-weight:400;">We noticed a new login to your account.</p>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="padding: 8px 24px 20px 24px;">
                            <!-- device icon -->
                            <div style="margin: 18px 0 10px 0;">
                                <svg width="48" height="48" fill="none" xmlns="http://www.w3.org/2000/svg">
                                  <circle cx="24" cy="24" r="22" stroke="#ccc" stroke-width="2"/>
                                  <rect x="16" y="12" width="16" height="24" rx="3" fill="#fafafd" stroke="#444" stroke-width="1"/>
                                  <circle cx="24" cy="32" r="2" fill="#808080"/>
                                </svg>
                            </div>
                            <p style="margin: 0; font-size:16px; color:#3d4253;">
                                <b>Device</b> · %s<br>
                                <span style="color:#888; font-size:15px;"><b>Location</b>: %s</span><br>
                                <span style="color:#888; font-size:15px;"><b>Time</b>: %s</span>
                            </p>
                            <p style="margin: 30px 0 12px 0; color:#606770; font-size:14px;">If this was you, you won’t be able to access certain security and account settings for a few days. You can still access these settings from a device you’ve logged in with in the past.</p>
                            <hr style="border:none;border-top:1px solid #e0e0e0; margin:14px 0;">
                            <p style="color:#606770; font-size:15px; margin:18px 0 8px 0;">
                                If this wasn't you, you can
                                <a href="YOUR_SECURITY_LINK" style="color:#316ff6; text-decoration:none;">secure your account</a>
                                from a device you’ve logged in with in the past.<br>
                                <a href="YOUR_LEARN_MORE_LINK" style="color:#316ff6; text-decoration:underline;">Learn more</a>
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="font-size:11px; color:#aaa; padding: 12px 24px 26px 24px;">
                            <span>
                                This message was produced and distributed by Jharkhand News, Ranchi, Jharkhand 834001. All rights reserved.<br/>
                                <a href="YOUR_PRIVACY_LINK" style="color:#316ff6;text-decoration:none;">privacy policy</a> |
                                <a href="YOUR_SUPPORT_LINK" style="color:#316ff6;text-decoration:none;">Contact Support</a>
                            </span>
                        </td>
                    </tr>
                </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(device, location, dateTime);
    }



}
