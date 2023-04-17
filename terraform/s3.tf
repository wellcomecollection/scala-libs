resource "aws_s3_bucket" "releases" {
  bucket = "releases.mvn-repo.wellcomecollection.org"
}

resource "aws_s3_bucket_acl" "releases" {
  bucket = aws_s3_bucket.releases.id
  acl    = "public-read"
}

resource "aws_s3_bucket_lifecycle_configuration" "releases" {
  bucket = aws_s3_bucket.releases.id

  rule {
    id      = "transition_all_to_standard_ia"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

resource "aws_s3_bucket_policy" "allow_public_reads" {
  bucket = aws_s3_bucket.releases.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.releases.arn}/*"
      },
      {
        Effect    = "Allow"
        Principal = { AWS = "760097843905" }
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.releases.arn}/*"
      }
    ] }
  )
}
