# Rabitto Pet Store Backend

API backend para gerenciamento de tutores e pets, além de autenticação baseada em JWT com refresh tokens.

## Visão Geral

Este projeto é uma API REST em Spring Boot utilizando PostgreSQL e JPA/Hibernate.

Principais funcionalidades:

- CRUD de Tutores (`/tutores`)
- CRUD de Pets (`/pets`)
- Endpoints de autenticação (`/auth/login`, `/auth/refresh`, `/auth/logout`)
- Fluxo com access token (JWT) + rotação de refresh token

## Stack Tecnológica

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Lombok
- JJWT (geração de tokens JWT)
- Docker + Docker Compose

## Estrutura do Projeto

```text
src/main/java/com/rabitto/backend
	controllers/
		AuthController.java
		PetController.java
		TutorController.java
	models/
		Auth.java
		Pet.java
		Tutor.java
	repositories/
		AuthRepository.java
		PetRepository.java
		TutorRepository.java
src/main/resources
	application.properties
```

## Resumo do Modelo de Dados

### Tutor

- `id` (Long)
- `nome` (String)
- `email` (String, único)
- `senha` (String)
- `telefone` (String, único)

### Pet

- `id` (Long)
- `nome` (String)
- `raca` (String)
- `porte` (String)
- `tutor` (ManyToOne -> Tutor)

### Auth (sessão de refresh token)

- `id` (Long)
- `tutor` (ManyToOne -> Tutor)
- `refreshToken` (String, único)
- `expiresAt` (Instant)
- `revoked` (boolean)
- `createdAt` (Instant)

## URL Base da API

Rodando localmente:

- `http://localhost:8080`

## Endpoints

### Auth

#### POST /auth/login

Requisição:

```json
{
  "email": "tutor@email.com",
  "senha": "123456"
}
```

Resposta (200):

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### POST /auth/refresh

Requisição:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Resposta (200): novo access token + novo refresh token.

#### POST /auth/logout

Requisição:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Resposta (200): revoga a sessão do refresh token.

### Tutores

- `GET /tutores` -> listar tutores
- `POST /tutores` -> criar tutor
- `PUT /tutores/{id}` -> atualizar tutor
- `DELETE /tutores/{id}` -> deletar tutor

Exemplo de body `POST /tutores`:

```json
{
  "nome": "Raul",
  "email": "raul@email.com",
  "senha": "123456",
  "telefone": "11999999999"
}
```

### Pets

- `GET /pets` -> listar pets
- `POST /pets` -> criar pet
- `PUT /pets/{id}` -> atualizar pet
- `DELETE /pets/{id}` -> deletar pet

Exemplo de body `POST /pets`:

```json
{
  "nome": "Thor",
  "raca": "Labrador",
  "porte": "Grande",
  "tutor": {
    "id": 1
  }
}
```

## Configuração

Valores padrão em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rabitto_db
spring.datasource.username=postgres
spring.datasource.password=postgres

app.auth.jwt-secret=${APP_AUTH_JWT_SECRET:change-this-secret-key-with-at-least-32-characters}
app.auth.access-token-minutes=15
app.auth.refresh-token-days=7
```

Importante:

- `APP_AUTH_JWT_SECRET` deve ter pelo menos 32 bytes.
- Em produção, sempre use um secret forte e credenciais seguras para o banco.

## Rodar Localmente (sem Docker)

Pré-requisitos:

- Java 21
- Maven (ou usar `mvnw`)
- PostgreSQL rodando em `localhost:5432`

Passos:

```bash
sh mvnw spring-boot:run
```

## Rodar com Docker Compose

Este repositório inclui:

- `Dockerfile` para build da imagem do backend
- `docker-compose.yml` para backend + PostgreSQL

Subir tudo em modo detached:

```bash
docker compose up -d --build
```

Parar e remover containers:

```bash
docker compose down
```

Parar e remover containers + volume do banco:

```bash
docker compose down -v
```

Comandos úteis:

```bash
docker compose logs -f backend
docker compose logs -f postgres
docker compose ps
```

Após iniciar:

- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

## Fluxo Rápido de Auth (curl)

Login:

```bash
curl -X POST http://localhost:8080/auth/login \
	-H "Content-Type: application/json" \
	-d '{"email":"raul@email.com","senha":"123456"}'
```

Refresh:

```bash
curl -X POST http://localhost:8080/auth/refresh \
	-H "Content-Type: application/json" \
	-d '{"refreshToken":"<YOUR_REFRESH_TOKEN>"}'
```

Logout:

```bash
curl -X POST http://localhost:8080/auth/logout \
	-H "Content-Type: application/json" \
	-d '{"refreshToken":"<YOUR_REFRESH_TOKEN>"}'
```

## Observações

- Os endpoints de CRUD ainda não estão protegidos por filtro JWT.
- A validação de senha aceita hashes BCrypt e fallback em texto puro.
- Considere adicionar uma ferramenta de migração (Flyway/Liquibase) para versionamento do schema.
