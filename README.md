# Service Discovery y Circuit Breaker con Resilience4j

- Juan Esteban Duque Taborda 
- Santiago Castillo Llanos 
- Juan Pablo Ordoñez Jimenez 



Taller de patrones de arquitectura de microservicios. Tres servicios Spring Boot se descubren entre sí con **Eureka**. El catálogo se protege con un **Circuit Breaker** de **Resilience4j** para que siga respondiendo cuando el servicio de cursos se cae.

## Objetivo

- Entender cómo un microservicio encuentra a otro por nombre, sin URLs fijas (Service Discovery).
- Implementar el patrón Circuit Breaker para evitar fallos en cascada y responder con un *fallback*.

## Arquitectura

| Servicio | Carpeta | Puerto | Rol |
|---|---|---|---|
| `fx-discovery-server` | [FutureXEurekaServer](FutureXEurekaServer) | 8761 | Servidor Eureka donde se registran los servicios |
| `fx-course-service` | [FutureXCourseApp](FutureXCourseApp) | 8001 | API de cursos con base de datos H2 en memoria y 3 cursos de ejemplo |
| `fx-catalog-service` | [FutureXCourseCatalog](FutureXCourseCatalog) | 8002 | Consume `fx-course-service` mediante Eureka y está protegido con Circuit Breaker |

```
fx-catalog-service ──(1) ¿dónde está fx-course-service?──▶ Eureka (8761)
        │                                                     ▲
        └──(2) GET http://<host>:8001/courses ──▶ fx-course-service ──┘ (se registra)
```

## Cómo funciona

**Service Discovery.** `fx-course-service` se registra en Eureka al arrancar. El catálogo le pregunta a Eureka por ese nombre y llama a la URL que recibe:

```java
InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service", false);
String courseAppURL = instanceInfo.getHomePageUrl();
```

**Circuit Breaker.** Los tres endpoints de [CatalogController](FutureXCourseCatalog/src/main/java/com/futurex/services/FutureXCourseCatalog/CatalogController.java) usan `@CircuitBreaker(name = "courseService", fallbackMethod = ...)`. Así funciona según la configuración de [application.properties](FutureXCourseCatalog/src/main/resources/application.properties):

- El circuito mira las últimas 10 llamadas. Se abre (`OPEN`) cuando ha habido al menos 5 llamadas y fallan el 50 % o más.
- Mientras está abierto, no llama a `fx-course-service` y responde enseguida con el fallback.
- A los 5 s pasa a `HALF_OPEN` y deja pasar 3 llamadas de prueba. Si salen bien, vuelve a `CLOSED`.

## Requisitos

- Java 17
- No hace falta instalar Maven: cada proyecto trae su propio `mvnw`.

## Cómo ejecutar

Abre una terminal por servicio y arráncalos **en este orden**:

```bash
cd FutureXEurekaServer   && ./mvnw spring-boot:run
cd FutureXCourseApp      && ./mvnw spring-boot:run
cd FutureXCourseCatalog  && ./mvnw spring-boot:run
```

En Windows usa `mvnw.cmd`. También puedes ejecutar la clase `*Application` de cada proyecto desde el IDE.

## Endpoints

| URL | Resultado |
|---|---|
| http://localhost:8761/ | Panel de Eureka con `FX-COURSE-SERVICE` y `FX-CATALOG-SERVICE` registrados |
| http://localhost:8002/ | Bienvenida del catálogo junto con el mensaje del servicio de cursos |
| http://localhost:8002/catalog | Lista de cursos |
| http://localhost:8002/firstcourse | Nombre del primer curso |
| http://localhost:8002/actuator/health | Estado del circuit breaker `courseService` (`CLOSED` / `OPEN` / `HALF_OPEN`) |

## Probar el Circuit Breaker

1. Con los tres servicios arriba, abre `/catalog`. Deberías ver los cursos.
2. Detén `fx-course-service`.
3. Abre `/catalog` varias veces. Responde `Fallback: Unable to retrieve course catalog.`
4. Revisa `/actuator/health`. Después de unas 5 llamadas fallidas, `courseService` aparece en `OPEN`.
5. Vuelve a arrancar `fx-course-service` y espera a que se registre en Eureka (unos 30 s). El circuito pasa a `HALF_OPEN`, luego a `CLOSED`, y `/catalog` vuelve a mostrar los cursos.

## Qué se logra

- Los servicios se comunican sin URLs fijas: si cambia el host o el puerto de `fx-course-service`, el catálogo lo encuentra igual a través de Eureka.
- Si `fx-course-service` se cae, el catálogo no se cae con él: responde con un mensaje controlado.
- Mientras el circuito está abierto, el catálogo deja de llamar a un servicio caído. Así no se gastan hilos ni se espera a timeouts, y el servicio dañado tiene tiempo de recuperarse.
- El estado del circuito se puede monitorear con Actuator.

## Tecnologías

Spring Boot 3.3.3 · Spring Cloud 2023.0.3 · Netflix Eureka · Resilience4j · Spring Data JPA + H2 · Spring Boot Actuator

---

Proyectos base del profesor: [jrquinte/FutureXEurekaServer](https://github.com/jrquinte/FutureXEurekaServer), [jrquinte/FutureXCourseApp](https://github.com/jrquinte/FutureXCourseApp), [jrquinte/FutureXCourseCatalog](https://github.com/jrquinte/FutureXCourseCatalog).
