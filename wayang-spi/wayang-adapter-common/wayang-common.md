
## 3. WAYANG-COMMON MODULE

```
wayang-common/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/common/
    │   │       ├── util/
    │   │       │   ├── DateTimeUtils.java
    │   │       │   ├── JsonUtils.java
    │   │       │   ├── StringUtils.java
    │   │       │   └── UUIDUtils.java
    │   │       │
    │   │       ├── exception/
    │   │       │   ├── WayangException.java
    │   │       │   ├── ResourceNotFoundException.java
    │   │       │   ├── ValidationException.java
    │   │       │   └── ErrorCode.java
    │   │       │
    │   │       ├── validation/
    │   │       │   ├── ValidationResult.java
    │   │       │   ├── ValidationError.java
    │   │       │   └── Validator.java
    │   │       │
    │   │       ├── event/
    │   │       │   ├── Event.java
    │   │       │   ├── EventType.java
    │   │       │   └── EventPublisher.java
    │   │       │
    │   │       └── config/
    │   │           ├── ConfigProvider.java
    │   │           └── Constants.java
    │   │
    │   └── resources/
    │       └── META-INF/
    │           └── beans.xml
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/common/
                └── util/
                    └── JsonUtilsTest.java