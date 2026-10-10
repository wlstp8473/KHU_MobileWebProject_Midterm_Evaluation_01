# Create your tests here.
from django.contrib.auth.models import User
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from PIL import Image
from io import BytesIO
from tempfile import TemporaryDirectory


class PostAPITest(APITestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username="testuser",
            password="testpassword"
        )

        self.url = "/api_root/Post/"

        self.post = Post.objects.create(
            author=self.user,
            title="Test Post",
            text="API test post",
            published_date=timezone.now()
        )

    def test_get_post_list(self):
        """게시물 목록 GET 요청 테스트"""

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]["title"], "Test Post")

    def test_create_post(self):
        """게시물 POST 생성 테스트"""

        self.client.force_authenticate(user=self.user)

        data = {
            "author": self.user.id,
            "title": "Created Post",
            "text": "Created by API test",
            "created_date": timezone.now(),
            "published_date": timezone.now(),
        }

        response = self.client.post(
            self.url,
            data,
            format="multipart"
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(
            Post.objects.filter(title="Created Post").exists()
        )

    def test_create_post_without_title(self):
        """필수 데이터가 없는 POST 요청 테스트"""

        self.client.force_authenticate(user=self.user)

        data = {
            "author": self.user.id,
            "text": "Title is missing"
        }

        response = self.client.post(
            self.url,
            data,
            format="multipart"
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_create_post_with_image_and_published_date(self):
        """앱 업로드와 같은 multipart 이미지·본문·게시 날짜를 검증한다."""
        self.client.force_authenticate(user=self.user)
        image_bytes = BytesIO()
        Image.new("RGB", (8, 8), "green").save(image_bytes, format="PNG")
        image = SimpleUploadedFile("upload.png", image_bytes.getvalue(), content_type="image/png")
        with TemporaryDirectory() as media_dir, override_settings(MEDIA_ROOT=media_dir):
            response = self.client.post(self.url, {
                "author": self.user.pk,
                "title": "앱에서 입력한 제목",
                "text": "앱에서 입력한 본문",
                "published_date": timezone.now().isoformat(),
                "image": image,
            }, format="multipart")
            self.assertEqual(response.status_code, status.HTTP_201_CREATED)
            post = Post.objects.get(title="앱에서 입력한 제목")
            self.assertTrue(post.image.storage.exists(post.image.name))
            self.assertIsNotNone(post.published_date)
            self.assertEqual(post.text, "앱에서 입력한 본문")
            self.assertContains(self.client.get("/"), "앱에서 입력한 제목")

    def test_create_post_with_unknown_author(self):
        self.client.force_authenticate(user=self.user)
        response = self.client.post(self.url, {
            "author": 999999,
            "title": "잘못된 작성자",
            "text": "저장되면 안 되는 게시물",
        }, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("author", response.data)
