# 🧠 AI-KB-Chat

> **An intelligent knowledge-aware chatbot leveraging AWS Bedrock Knowledge Base and Amazon S3 Vector Store for context-driven enterprise Q&A**

---

## 🚀 Tech Stack & Features

| Technology | Purpose |
|-------------|----------|
| ![Angular](https://img.shields.io/badge/Angular-16-red?logo=angular&logoColor=white) | Frontend SPA built using Angular 16 and Angular Material |
| ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-green?logo=springboot&logoColor=white) | Backend REST API layer |
| ![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk&logoColor=white) | Core business logic |
| ![AWS Bedrock](https://img.shields.io/badge/AWS-Bedrock-orange?logo=amazonaws&logoColor=white) | Foundation model access layer |
| ![Knowledge Base Retrieval API](https://img.shields.io/badge/Knowledge%20Base-Retrieval%20API-yellow?logo=amazonaws&logoColor=black) | Retrieves contextually relevant data |
| ![Amazon S3 Vector Store (Preview)](https://img.shields.io/badge/Amazon%20S3-Vector%20Store%20Preview-795548?logo=amazonaws&logoColor=white) | Vector embeddings & document storage |
| ![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-f79400?logo=awslambda&logoColor=white) | Serverless runtime |
| ![API Gateway](https://img.shields.io/badge/API-Gateway-maroon?logo=amazonaws&logoColor=white) | API exposure & routing |
| ![CloudFront](https://img.shields.io/badge/AWS-CloudFront-purple?logo=amazonaws&logoColor=white) | Secure global distribution |

---

## 🧩 Architecture

The system enables users to chat with an enterprise knowledge base through an Angular UI.  
Requests flow through the Spring Boot API → AWS Bedrock Knowledge Base → Amazon S3 Vector Store (Preview).

![Architecture](docs/architecture-ai-kb-chat.png)

> *(Generated architecture diagram — included below for reference)*  
> ![Architecture Diagram](A_README_file_in_digital_medium_introduces_"AI-KB-.png)

---

## 🧠 Key Highlights

- **Bedrock Knowledge Base Integration:**  
  Uses Retrieval-Augmented Generation (RAG) to provide precise, source-aware answers.  
- **Amazon S3 Vector Store (Preview):**  
  Cost-efficient alternative to OpenSearch for vector storage.  
- **Spring Boot + Angular Monorepo:**  
  Seamless full-stack communication and simplified deployment.  
- **Secure Cloud Distribution:**  
  Delivered through AWS CloudFront + Route 53 + S3.  
- **Scalable Serverless Backend:**  
  Uses AWS Lambda with API Gateway for zero-maintenance scaling.

---

## 🧭 Folder Structure

```
ai-kb-chat/
│
├── frontend/                # Angular 16 SPA
│   ├── src/
│   └── dist/
│
├── backend/                 # Spring Boot REST backend
│   ├── src/main/java/com/dct/kb/ai_chat/
│   └── target/
│
├── template.yml             # SAM template for AWS Lambda deployment
└── README.md
```

---

## 🖼️ Screenshots

> _Placeholder for screenshots_  
> (Chat UI, Knowledge Base Explorer, Response Flow, etc.)

---

## 🧾 License

Distributed under the **MIT License**.

---

### 👤 Author
**DCT (Durga Charan Tadi)**  
🚀 _Building intelligent cloud-native apps integrating AI + AWS + Java + Angular_
