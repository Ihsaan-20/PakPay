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

<img width="1366" height="614" alt="screencapture-localhost-5173-login-2026-05-12-12_47_32" src="https://github.com/user-attachments/assets/9a659e4e-7f9d-43ec-8ec3-52103b5cd548" />
<img width="1366" height="871" alt="screencapture-localhost-5173-dashboard-2026-05-12-13_01_09" src="https://github.com/user-attachments/assets/0a47495e-c8c3-4560-a8ba-190fb654294a" />
<img width="1366" height="871" alt="screencapture-localhost-5173-dashboard-2026-05-12-13_01_19" src="https://github.com/user-attachments/assets/52c1c922-a9b4-4349-8cbc-1860f2e5b7e3" />
<img width="1366" height="871" alt="screencapture-localhost-5173-dashboard-2026-05-12-13_01_30" src="https://github.com/user-attachments/assets/c7ccdbab-8198-4281-a26a-98b226c366a3" />
<img width="1366" height="871" alt="screencapture-localhost-5173-dashboard-2026-05-12-13_02_07" src="https://github.com/user-attachments/assets/9d438464-f7ea-4025-aa2a-3d4d94d886f4" />
<img width="1366" height="871" alt="screencapture-localhost-5173-dashboard-2026-05-12-13_02_32" src="https://github.com/user-attachments/assets/c965db00-0a5b-4484-9a8b-35dcbc82891c" />



