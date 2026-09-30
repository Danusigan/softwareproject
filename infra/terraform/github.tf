# Lets the GitHub Actions deploy job act in this AWS account without any stored AWS keys:
# the workflow exchanges GitHub's short-lived OIDC token for this role's credentials.
#
# The role can only add/remove ingress rules on the app server's security group - enough for
# a deploy to open SSH to its own runner IP for the duration of the deploy, nothing more.
# Only pushes to main of var.github_repo can assume it (not PRs, not forks, not other branches).

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  # GitHub's published intermediate CA thumbprints (AWS also validates via its own trust store).
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]
}

data "aws_iam_policy_document" "github_deploy_trust" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "${var.project}-github-deploy"
  assume_role_policy   = data.aws_iam_policy_document.github_deploy_trust.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    actions = [
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:RevokeSecurityGroupIngress",
    ]
    resources = [aws_security_group.app.arn]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "open-ssh-for-deploy"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}
