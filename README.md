# OpenClaw AI Server Hub 🤖🌐

![Banner](https://img.shields.io/badge/Status-Active-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Android_28%2B-blue.svg)

OpenClaw is a powerful Android application that transforms your smartphone into a **Personal AI Server Hub**. It is not just an AI chat app; it is a full-fledged local server that exposes Web UI, Telegram Bot capabilities, and Model Context Protocol (MCP) endpoints right from your pocket!

OpenClaw เป็นแอปพลิเคชัน Android ที่ทรงพลังซึ่งจะเปลี่ยนสมาร์ทโฟนของคุณให้กลายเป็น **เซิร์ฟเวอร์ AI ส่วนตัว (AI Server Hub)** ไม่ใช่แค่แอปแชต AI ทั่วไป แต่มันคือเซิร์ฟเวอร์แบบพกพาที่ให้บริการ Web UI, Telegram Bot และเชื่อมต่อผ่าน Model Context Protocol (MCP) ได้จากกระเป๋ากางเกงของคุณ!

---

## Features / ฟีเจอร์หลัก ✨

- **Server Mode (Web App)**: Runs a Ktor Web Server (Port 8080) on your device. Connect from any PC/Phone on the same Wi-Fi. / รัน Ktor Web Server ในตัวแอป เข้าถึง AI ผ่านเบราว์เซอร์จากเครื่องอื่นได้
- **Telegram Bot Integration**: Acts as a Telegram Bot host using long-polling. Chat with your home Android device from anywhere. / ใช้มือถือเป็นเซิร์ฟเวอร์รันบอท Telegram ตลอดเวลา
- **System Widget (Glance)**: Monitor CPU, RAM, and Battery straight from your Android Home Screen. / วิดเจ็ตหน้าจอมือถือแสดงสถานะระบบแบบ Real-time
- **Multimodal Vision**: Take photos or pick images to analyze them with Vision models (e.g. GPT-4o). / อัปโหลดรูปภาพให้ AI ช่วยวิเคราะห์ได้
- **Voice System (STT/TTS)**: Talk to the AI and have it read responses out loud. / รองรับระบบสั่งงานด้วยเสียงและให้ AI อ่านออกเสียง
- **Ultimate Customization**: Custom hex colors, custom background images, fonts, and themes. / ปรับแต่งสีแอปและพื้นหลังห้องแชตได้อิสระ

## How to Install and Run / วิธีติดตั้งและการใช้งาน 🚀

### 1. Build and Install (ติดตั้งแอป)
* Open the project in **Android Studio**. / เปิดโปรเจกต์ด้วย Android Studio
* Connect your Android device (Android 9.0+ / API 28+). / เสียบสายมือถือ (รองรับ Android 9 ขึ้นไป)
* Click **Run** (`Shift + F10`) to build and install the APK. / กดรันเพื่อติดตั้ง

### 2. Basic Setup (การตั้งค่าพื้นฐาน)
* Open the App, tap on the **Settings (Gear Icon)**. / เปิดแอป กดไอคอนตั้งค่า
* Enter your **API Key** (e.g., OpenAI, Anthropic, Gemini). / ใส่ API Key ของคุณ
* Select your preferred Model. / เลือกรุ่น AI ที่ต้องการใช้งาน

### 3. Server Mode & Web App (การใช้งานโหมดเซิร์ฟเวอร์)
* In Settings, scroll down to **Server & Telegram Mode**. / ไปที่ตั้งค่า เลื่อนหาโหมดเซิร์ฟเวอร์
* Tap **Start Server Hub**. / กดปุ่มเริ่มเซิร์ฟเวอร์
* Open a browser on any device in the same Wi-Fi and go to `http://<your-phone-ip>:8080`. / เปิดเว็บเบราว์เซอร์ในวง LAN เดียวกัน พิมพ์ IP เครื่องมือถือพอร์ต 8080
* Enjoy the gorgeous Glassmorphism Web App UI! / ใช้งาน Web App สุดล้ำได้ทันที!

### 4. Telegram Bot (บอทเทเลแกรม)
* Enter your Telegram Bot Token in Settings. / ใส่โทเคนของ Telegram Bot ในหน้าตั้งค่า
* Start the Server. The app will act as the brain for your Bot! / เปิดเซิร์ฟเวอร์ แล้วแอปจะทำงานเป็นสมองให้บอทของคุณทันที

*(Note: Replace `YOUR_USERNAME` and the repo name with your actual GitHub repository URL / อย่าลืมเปลี่ยนลิงก์เป็น Repository ของคุณเอง)*

---
**Need Help or Found a Bug? (ติดต่อ/แจ้งปัญหา)**
Telegram: [@benzsirirat](https://t.me/benzsirirat)
