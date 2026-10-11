# 모바일 웹서비스 프로젝트 중간평가 01

모바일/웹서비스 프로젝트 중간평가 과제로 만든 포토 블로그입니다. 서버는 Django REST Framework, 앱은 Android Java로 구현했습니다. 앱에서 서버의 이미지를 불러오고, 제목·본문·이미지를 입력해 새 게시물을 올릴 수 있습니다.

- GitHub: [KHU_MobileWebProject_Midterm_Evaluation_01](https://github.com/wlstp8473/KHU_MobileWebProject_Midterm_Evaluation_01)
- 클라우드 웹 페이지: [kimjinse.pythonanywhere.com](https://kimjinse.pythonanywhere.com/)
- 클라우드 게시물 API: [api_root/Post/](https://kimjinse.pythonanywhere.com/api_root/Post/)

평가 항목별 실행 화면은 루트 폴더의 `모바일웹서비스 프로젝트_공통평가 01_수행 결과 보고서.docx`에 정리했습니다.

## 프로젝트 구조

```text
KHU_MobileWebProject_Midterm_Evaluation_01/
├── PhotoBlogServer/                         # Django 서버
│   ├── blog/                                # 모델, API, 웹 화면, 테스트
│   ├── mysite/                              # Django 설정과 URL 구성
│   ├── media/                               # 기본 이미지 및 업로드 저장 위치
│   ├── manage.py
│   └── requirements.txt
├── PhotoViewer/                             # Android 앱
│   ├── app/src/main/java/com/example/photoviewer/
│   │   ├── MainActivity.java                # 화면, 서버 선택, 동기화, 업로드
│   │   ├── PostItem.java                    # 게시물 정보
│   │   ├── PostListViewModel.java           # 동기화 목록과 검색어 유지
│   │   ├── PostDetailActivity.java          # 게시물 상세 화면
│   │   ├── PostUploader.java                # multipart POST 전송
│   │   └── ImageAdapter.java                # 이미지 목록 표시
│   ├── app/src/main/res/layout/             # 메인 화면과 업로드 입력창
│   └── app/build.gradle.kts                 # BuildConfig 생성 설정
├── 모바일웹서비스 프로젝트_공통평가 01_수행 결과 보고서.docx
└── README.md
```

## 실행 환경

| 구분 | 버전 및 설정 |
|---|---|
| 로컬 서버 | Python 3.11 |
| 클라우드 서버 | PythonAnywhere, Python 3.13 |
| 서버 패키지 | `PhotoBlogServer/requirements.txt` 참조 |
| Android SDK | compileSdk / targetSdk 37, minSdk 24 |
| Android Gradle Plugin | 9.3.3 |
| Gradle Wrapper | 9.5.0 |
| Gradle Daemon JVM | JDK 25 (`gradle-daemon-jvm.properties`) |
| 에뮬레이터 | API 37 |

Android Studio에 SDK 37과 에뮬레이터 이미지를 설치해야 합니다. Gradle 실행용 JDK는 25, Java 소스 호환성은 11로 설정되어 있습니다.

아래 명령은 Windows PowerShell 기준입니다.

## 저장소 내려받기

```powershell
git clone https://github.com/wlstp8473/KHU_MobileWebProject_Midterm_Evaluation_01.git
cd KHU_MobileWebProject_Midterm_Evaluation_01
```

가상환경(`venv`), DB(`db.sqlite3`), 개인 설정(`local.properties`)은 저장소에 올리지 않았습니다. 처음 실행하는 PC에서는 아래 순서대로 생성하면 됩니다. 로컬 서버와 클라우드 서버의 DB·토큰은 서로 다릅니다.

## 로컬 서버 실행

저장소 루트에서 실행합니다. `py -3.11`이 인식되지 않으면 설치된 Python 실행 경로를 사용하세요.

```powershell
cd PhotoBlogServer
py -3.11 -m venv venv
.\venv\Scripts\python.exe -m pip install -r requirements.txt
.\venv\Scripts\python.exe manage.py migrate
.\venv\Scripts\python.exe manage.py createsuperuser
```

`YOUR_ADMIN_USERNAME`에 방금 만든 관리자 계정의 사용자명을 넣어 토큰을 확인한 뒤 서버를 실행합니다.

```powershell
.\venv\Scripts\python.exe manage.py drf_create_token YOUR_ADMIN_USERNAME
.\venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

앱을 사용하는 동안 서버 터미널을 켜 두어야 합니다.

| 용도 | PC 브라우저 주소 |
|---|---|
| 블로그 | http://127.0.0.1:8000/ |
| 관리자 | http://127.0.0.1:8000/admin/ |
| API 루트 | http://127.0.0.1:8000/api_root/ |
| 게시물 GET / POST | http://127.0.0.1:8000/api_root/Post/ |

처음 만든 DB에는 게시물이 없습니다. 관리자 화면이나 게시물 API에서 이미지를 등록한 뒤 앱의 동기화를 확인하세요. 웹 블로그에 표시하려면 게시 날짜도 입력해야 하며, 미래 날짜의 게시물은 아직 목록에 나오지 않습니다.

## Android 앱 설정

Android Studio에서 **`PhotoViewer` 폴더**를 엽니다.

### SDK 경로와 토큰

`PhotoViewer/local.properties`에 SDK 경로와 토큰을 입력합니다. Android Studio가 생성한 `sdk.dir`는 그대로 사용하면 됩니다.

```properties
# PC의 SDK 설치 경로
sdk.dir=C:/Android/Sdk

# 각 서버에서 발급한 토큰으로 변경
LOCAL_AUTH_TOKEN=REPLACE_WITH_LOCAL_SERVER_TOKEN
CLOUD_AUTH_TOKEN=REPLACE_WITH_CLOUD_SERVER_TOKEN
```

토큰은 따옴표 없이 **40자리 16진수**로 입력합니다. 현재 Gradle 설정에서는 두 값을 모두 검사하므로, 한쪽 서버만 사용하더라도 두 항목을 채워야 Sync가 완료됩니다.

로컬 토큰은 앞의 `drf_create_token` 명령으로 확인할 수 있습니다. 클라우드 토큰은 PythonAnywhere의 서버 가상환경에서 같은 명령을 실행하거나, 클라우드 관리자 화면의 Tokens에서 확인합니다. 클라우드 서버에 접근할 수 없다면 관리자에게 토큰을 받아야 합니다.

앱에서는 빌드할 때 생성되는 아래 값을 사용합니다.

```java
BuildConfig.LOCAL_AUTH_TOKEN
BuildConfig.CLOUD_AUTH_TOKEN
```

토큰은 `local.properties`에서 관리하고 Java 소스나 README에는 넣지 않았습니다. 다만 BuildConfig 값은 APK에도 포함되므로, 토큰을 앱 안에서 숨기는 방식은 아닙니다.

### 연결할 서버 선택

`PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`의 `USE_CLOUD` 값을 변경합니다.

```java
private static final boolean USE_CLOUD = true;
```

| 값 | 앱이 연결하는 게시물 API | 사용하는 토큰 |
|---|---|---|
| `true` (현재 설정) | `https://kimjinse.pythonanywhere.com/api_root/Post/` | `CLOUD_AUTH_TOKEN` |
| `false` | `http://10.0.2.2:8000/api_root/Post/` | `LOCAL_AUTH_TOKEN` |

`USE_CLOUD`를 바꾼 뒤 앱을 다시 빌드해서 실행합니다. 토큰이나 Gradle 설정을 바꿨다면 먼저 Gradle Sync를 진행하세요.

PC 브라우저는 `127.0.0.1:8000`, 에뮬레이터는 `10.0.2.2:8000`으로 로컬 서버에 접속합니다. 실제 휴대전화에서 테스트하려면 PC의 LAN 주소를 사용하고 서버 실행 주소와 `ALLOWED_HOSTS`, 네트워크 설정도 변경해야 합니다.

### 앱 실행과 사용

1. Android Studio에서 **Sync Project with Gradle Files**를 실행합니다.
2. 에뮬레이터를 시작하고 `app`을 Run합니다.
3. 로컬 모드라면 PC의 Django 서버가 실행 중인지 확인합니다.
4. **동기화**를 누르면 서버의 게시물 정보와 이미지를 가져옵니다.
5. **새로운 이미지 게시**를 누르고 작성자 ID·제목·본문을 입력합니다.
6. **이미지 선택**에서 파일을 선택하고 미리보기를 확인합니다.
7. **게시**를 누르면 multipart POST로 전송하며, 성공 후 목록을 자동 동기화합니다.

목록에는 이미지와 함께 제목·게시 날짜가 표시됩니다. 제목을 누르면 상세 화면에서 본문, 작성자 ID, 작성·게시 날짜를 볼 수 있습니다. 날짜는 기기의 시간대로 표시합니다. 긴 본문은 스크롤해서 읽고, **목록으로** 버튼이나 뒤로 가기를 누르면 이전 목록으로 돌아옵니다.

**큰 이미지 / 썸네일 목록**에서 보기 방식을 선택할 수 있습니다. 썸네일 목록은 작은 이미지 옆에 제목과 날짜를 표시해 여러 게시물을 한 화면에서 확인하기 편합니다. 선택은 앱을 다시 실행해도 유지되며, 검색 결과에도 적용됩니다. 표시 방식만 바꾸므로 이미지 다운로드 용량은 동일합니다.

검색창에 입력하면 동기화한 게시물의 제목·본문에서 해당 문구를 찾습니다. 영문 대소문자와 입력 앞뒤 공백은 구분하지 않습니다. **지우기**를 누르거나 검색어를 모두 삭제하면 전체 목록으로 돌아옵니다. 검색 중에 동기화하거나 게시물을 올려도 같은 검색어를 새 목록에 적용합니다. 서버 전체를 검색하는 기능은 아니므로, 새 게시물은 먼저 동기화해야 검색할 수 있습니다.

작성자 ID의 기본값은 `1`입니다. 다른 계정을 사용한다면 해당 서버의 관리자 화면에서 사용자 ID를 확인해 입력하세요. 제목·본문·이미지는 모두 필수이고, 이미지는 최대 20MB까지 올릴 수 있습니다. 작성·게시 날짜는 게시 시각으로 자동 입력됩니다.

API 37 이상에서는 로컬 네트워크 권한을 요청합니다. 현재 코드는 클라우드 모드에서도 이 권한을 확인하므로, 요청이 뜨면 허용해야 동기화와 게시를 진행할 수 있습니다.
