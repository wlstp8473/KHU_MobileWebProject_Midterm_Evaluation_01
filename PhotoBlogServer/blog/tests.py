# Create your tests here.
from django.contrib.auth.models import User
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post


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