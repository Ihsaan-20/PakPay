\# 💳 PakPay - Modern Digital Wallet Solution



PakPay is a secure, full-stack digital payment application designed to handle real-time financial transactions. Built with a focus on security and scalability, it mimics real-world banking standards like EMVCo QR codes and idempotent transaction processing.



\## 🚀 Key Features



\- \*\*Secure Transfers:\*\* Multi-step verification including Receiver Validation and \*\*Transaction PIN\*\* security.

\- \*\*EMVCo Standard QR:\*\* Generates professional \*\*EMV-compliant QR Codes\*\* with embedded \*\*NayaPay IBAN\*\* and CRC checksum.

\- \*\*Idempotency Logic:\*\* Prevents duplicate payments using custom `X-Idempotency-Key` headers.

\- \*\*Real-time Synchronization:\*\* Direct API integration for live balance updates, bypassing stale local storage data.

\- \*\*JWT Authentication:\*\* Secure stateless session management using Spring Security.

\- \*\*Dark UI Experience:\*\* Sleek, modern dashboard built with Tailwind CSS and Framer Motion.



\## 🛠️ Tech Stack



\### Backend

\- \*\*Java 22\*\* \& \*\*Spring Boot 3\*\*

\- \*\*Spring Security\*\* (JWT Authentication)

\- \*\*MySQL\*\* (Database)

\- \*\*Redis\*\* (Caching \& Optimization)

\- \*\*Lombok\*\* \& \*\*Maven\*\*



\### Frontend

\- \*\*React.js\*\* (Vite)

\- \*\*Tailwind CSS\*\* (Styling)

\- \*\*Framer Motion\*\* (Animations)

\- \*\*Lucide React\*\* (Icons)

\- \*\*Axios\*\* (API Interceptor)



\## 📂 Project Structure



```text

/PakPay-Project

&#x20; ├── /backend   # Spring Boot Application (Java 22)

&#x20; └── /frontend  # React Application (Vite)

