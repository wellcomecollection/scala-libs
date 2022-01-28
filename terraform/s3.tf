resource "aws_s3_bucket" "releases" {
  bucket = "releases.mvn-repo.wellcomecollection.org"
  acl    = "public-read"

  lifecycle_rule {
    id      = "transition_all_to_standard_ia"
    enabled = true

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

resource "aws_s3_bucket_policy" "allow_public_reads" {
  bucket = aws_s3_bucket.releases.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "${aws_s3_bucket.releases.arn}/*"
    }
  ]
}
EOF
}
