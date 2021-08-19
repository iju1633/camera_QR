# camera_QR
## 카메라 모듈을 구현
### 기능
- "촬영" 버튼을 눌러 카메라 앱 띄우기
    - onClick 메서드에 구현
- 권한 체크를 물어보는 창
    - TedPermission
    - 메시지도 추가
- 이미지를 뷰에 생성하고 디바이스에 저장하기
    - startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)을 통해 파일이 이동할 수 있게끔
- 사진의 이름을 사진이 찍힌 일시로 저장
    - 중복 제거
- 비트맵 사진 폴더 경로에 저장
    - onActivityResult에 구현
- 이미지 뷰에 이미지 띄우기
- 저장된 사진을 갤러리 폴더 반영 및 최신화
    - MediaScanning
- 사진 각도 자동 보정
    - 사진 촬영 시, user에 따라 다른 각도로 찍기 때문에 뷰에서 알아서 회전한 채로 보여주기 위함
    - exifOrientationToDegrees, rotate 메서드에 구현

### 부족한 점
- 메인 로직인 onActivityResult가 이해가 어려움
- 다른 앱과의 연동 방법을 잘 모르겠다
