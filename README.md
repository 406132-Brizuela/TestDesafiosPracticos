# TestDesafiosPracticos

TP de Progra IV - Desafíos Prácticos.

## Estructura del proyecto

```
/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/tp/desafiospracticos/
│       │   │   ├── DesafiosPracticosApplication.java
│       │   │   ├── engine/
│       │   │   │   ├── domain/
│       │   │   │   ├── profile/
│       │   │   │   ├── dimension/
│       │   │   │   ├── gate/
│       │   │   │   ├── aggregation/
│       │   │   │   ├── metrics/
│       │   │   │   └── analysis/
│       │   │   ├── challenge/
│       │   │   ├── submission/
│       │   │   ├── web/
│       │   │   └── config/
│       │   └── resources/
│       │       ├── application.properties
│       │       └── application-local.properties
│       └── test/java/com/tp/desafiospracticos/
└── frontend/
```

## Cómo levantar el proyecto

### Backend

Abrir la carpeta `/backend` desde IntelliJ.

```
cd backend
mvn spring-boot:run
```

### Frontend

Abrir la carpeta `/frontend` desde WebStorm.

```
cd frontend
npm start
```
