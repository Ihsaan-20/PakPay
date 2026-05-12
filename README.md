<img width="1366" height="768" alt="WhatsApp Image 2026-05-12 at 10 37 29 AM" src="https://github.com/user-attachments/assets/9b584a8d-5352-4b66-bccb-5d1c02efacd6" />
<img width="1366" height="768" alt="WhatsApp Image 2026-05-12 at 10 37 29 AM (1)" src="https://github.com/user-attachments/assets/a11912eb-f88f-4298-9ba6-1a1abe2c6e10" />
\# 💳 PakPay - Modern Digital Wallet Solution



PakPay is a secure, full-stack digital payment application designed to handle real-time financial transactions. Built with a focus on security and scalability, it mimics real-world banking standards like EMVCo QR codes and idempotent transaction processing.



\## 🚀 Key Features



\- \*\*Secure Transfers:\*\* Multi-step verification including Receiver Validation and \*\*Transaction PIN\*\* security.

\- \*\*EMVCo Standard QR:\*\* Generates professional \*\*EMV-compliant QR Codes\*\* and CRC checksum.

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

