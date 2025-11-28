# <h1>SwipeLab <img src="https://github.com/edenbar23/SwipeLab/blob/main/icon.png" width="30" /></h1> 
### *A Gamified Human-in-the-Loop Image Labeling Platform*

SwipeLab is a cross-platform image labeling system designed to help researchers quickly collect **high-quality human labels** for image datasets. The platform includes a **mobile app** for users to classify images using a simple “Yes / No / I Don’t Know” interface, a **secure backend** for data storage and API communication, and a **research dashboard** for monitoring label quality and exporting results.

---

## 📸 Project Overview

Machine learning models require large, **accurately labeled datasets**. Many classification tasks still depend on **human intuition**, especially when images involve subtle patterns, aesthetics, or subjective interpretation.

SwipeLab provides a **fast, intuitive, and gamified labeling interface** that makes it easy for casual users and volunteers to contribute. The system ensures data quality through credibility scoring, gold-standard items, and progress tracking.

---

## 🧩 System Components
```bash
SwipeLab/
│
├── frontend/ # React Native (Expo) mobile app
│ └── swipe-based classification UI
│
├── backend/ # Java Spring Boot server
│ ├── REST API for images, labels, authentication
│ ├── PostgreSQL database
│ └── researcher dashboard & admin endpoints
│
└── docs/ # Architecture diagrams, proposal, design docs
```


---

## 🎯 Goals of the Project

- Provide researchers with a **simple & scalable tool** for collecting human-labeled data.  
- Make labeling **fast, intuitive, and accessible** on any mobile device.  
- Support **human quality control** with gold-standard items and credibility scoring.  
- Deliver a secure backend with **auditing, authentication, and export tools**.  
- Build a gamified system that motivates users to contribute more labels.

---

## 📱 Frontend (React Native + Expo)

The mobile app allows users to:

- Swipe through images to classify them  
- Choose **Yes / No / I Don’t Know** with one tap  
- Track personal progress (points, badges, streaks)  
- Compete on leaderboards  
- Rate confidence and improve credibility score  
- View session history & statistics  

Designed for:  
✔ simplicity  
✔ speed  
✔ mobile-first  
✔ minimal cognitive load  

---

## 🔧 Backend (Spring Boot + PostgreSQL)

The backend provides:

- Secure REST API (HTTPS + OAuth2 Google Sign-In)
- Image batch retrieval
- Label submission & validation
- Gold image handling for quality control
- User accounts, roles, and credibility scoring
- Researcher dashboard with:
  - statistics & graphs  
  - dataset export (CSV/JSON)  
  - consensus levels  
  - user analytics  
  - error logs & audit trail  

---

## 🧪 Quality & Testing

- **Unit Tests:** frontend components & backend services  
- **Integration Tests:** app ↔ backend ↔ dataset API  
- **API Tests:** schema validation, authentication, rate limits  
- **UI/UX Tests:** usability and user studies with volunteers  
- **Performance Tests:** response times, batch processing  
- **User Acceptance Testing (UAT):** final evaluation by real researchers  

---

## 🔐 Security Requirements

- HTTPS-only  
- OAuth2 Google sign-in  
- Token validation  
- Short-lived signed URLs for images  
- CORS & client restrictions  
- Auditable logs (user, timestamp, label, elapsed time)  
- Role-based access (user vs researcher/admin)  

---

## 📚 Technologies

### **Frontend**
- React Native  
- Expo CLI  
- TypeScript  
- React Navigation  
- Reanimated & Gesture Handler  

### **Backend**
- Spring Boot  
- Spring Security + OAuth2  
- Maven / Gradle  
- PostgreSQL  
- Docker  
- AWS / GCP / Render  

### **Tools**
- GitHub + GitHub Actions CI/CD  
- pgAdmin  
- Postman  
- draw.io for architecture diagrams  

---

## 🚀 Getting Started

### Clone repo
```bash
git clone https://github.com/edenbar23/SwipeLab.git
```
### Setup Frontend
```bash
cd frontend
npm install
npm start
```
### Setup Backend
```bash
cd ../backend
./mvnw spring-boot:run
```

### 👥 Team
Sagi Evroni
Eden Bar
Ofri Hanochi
Avihoo Amos

### Customer:
Prof. Chen Keasar
Department of Interdisciplinary Computation
Ben-Gurion University

### 📄 License
TBD — will be added before public release.
cd SwipeLab
