#!/bin/bash

# Скрипт для налаштування IAM Service Account для EBS CSI Driver

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔧 НАЛАШТУВАННЯ IAM ДЛЯ EBS CSI DRIVER                     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

CLUSTER_NAME="andreychyk-bank-cluster"
REGION="eu-north-1"
SERVICE_ACCOUNT_NAME="ebs-csi-controller-sa"
NAMESPACE="kube-system"

# Отримати OIDC provider URL
echo "📋 Отримання OIDC provider URL..."
OIDC_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --region $REGION --query "cluster.identity.oidc.issuer" --output text | cut -d '/' -f 5)

if [ -z "$OIDC_ID" ]; then
    echo "❌ Не вдалося отримати OIDC provider ID"
    exit 1
fi

echo "✅ OIDC Provider ID: $OIDC_ID"

# Перевірити, чи існує OIDC provider
echo ""
echo "📋 Перевірка OIDC provider..."
aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[?contains(Arn, '$OIDC_ID')]" --output table

# Використати AWS managed policy для EBS CSI driver
echo ""
echo "📋 Використання AWS managed policy для EBS CSI driver..."

AWS_MANAGED_POLICY_ARN="arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
POLICY_ARN="$AWS_MANAGED_POLICY_ARN"

# Перевірити, чи існує managed policy
if aws iam get-policy --policy-arn "$AWS_MANAGED_POLICY_ARN" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ AWS managed policy знайдено: $AWS_MANAGED_POLICY_ARN${NC}"
else
    echo -e "${YELLOW}⚠️  AWS managed policy не знайдено, створюємо власну policy...${NC}"
    
    cat > /tmp/ebs-csi-driver-policy.json << 'POLICY_EOF'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:CreateSnapshot",
                "ec2:AttachVolume",
                "ec2:DetachVolume",
                "ec2:ModifyVolume",
                "ec2:DescribeAvailabilityZones",
                "ec2:DescribeInstances",
                "ec2:DescribeSnapshots",
                "ec2:DescribeTags",
                "ec2:DescribeVolumes",
                "ec2:DescribeVolumesModifications"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:CreateTags"
            ],
            "Resource": [
                "arn:aws:ec2:*:*:volume/*",
                "arn:aws:ec2:*:*:snapshot/*"
            ],
            "Condition": {
                "StringEquals": {
                    "ec2:CreateAction": [
                        "CreateVolume",
                        "CreateSnapshot"
                    ]
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteTags"
            ],
            "Resource": [
                "arn:aws:ec2:*:*:volume/*",
                "arn:aws:ec2:*:*:snapshot/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:CreateVolume"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "aws:RequestTag/ebs.csi.aws.com/cluster": "true"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:CreateVolume"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "aws:RequestTag/CSIVolumeName": "*"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteVolume"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/ebs.csi.aws.com/cluster": "true"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteVolume"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/CSIVolumeName": "*"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteVolume"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/kubernetes.io/created-for/pvc/name": "*"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteSnapshot"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/CSIVolumeSnapshotName": "*"
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DeleteSnapshot"
            ],
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "ec2:ResourceTag/ebs.csi.aws.com/cluster": "true"
                }
            }
        }
    ]
}
POLICY_EOF
    
    POLICY_NAME="Amazon_EBS_CSI_Driver_Policy_$CLUSTER_NAME"
    EXISTING_POLICY_ARN=$(aws iam list-policies --query "Policies[?PolicyName=='$POLICY_NAME'].Arn" --output text 2>/dev/null || echo "")
    
    if [ -z "$EXISTING_POLICY_ARN" ]; then
        echo "📝 Створення IAM policy..."
        POLICY_ARN=$(aws iam create-policy \
            --policy-name "$POLICY_NAME" \
            --policy-document file:///tmp/ebs-csi-driver-policy.json \
            --query 'Policy.Arn' \
            --output text 2>&1)
        echo "✅ Policy створено: $POLICY_ARN"
    else
        POLICY_ARN="$EXISTING_POLICY_ARN"
        echo "✅ Policy вже існує: $POLICY_ARN"
    fi
fi

# Створити IAM role для service account
echo ""
echo "📋 Створення IAM role для service account..."

OIDC_PROVIDER_ARN=$(aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[?contains(Arn, '$OIDC_ID')].Arn" --output text 2>/dev/null | head -1)

if [ -z "$OIDC_PROVIDER_ARN" ]; then
    echo "❌ OIDC provider не знайдено. Спробуйте встановити його вручну."
    exit 1
fi

ROLE_NAME="AmazonEKS_EBS_CSI_DriverRole_$CLUSTER_NAME"

# Створити trust policy
cat > /tmp/trust-policy.json << TRUST_EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "$OIDC_PROVIDER_ARN"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "${OIDC_PROVIDER_ARN#*/}:sub": "system:serviceaccount:$NAMESPACE:$SERVICE_ACCOUNT_NAME",
                    "${OIDC_PROVIDER_ARN#*/}:aud": "sts.amazonaws.com"
                }
            }
        }
    ]
}
TRUST_EOF

# Перевірити, чи існує role
EXISTING_ROLE=$(aws iam get-role --role-name "$ROLE_NAME" 2>/dev/null || echo "")

if [ -z "$EXISTING_ROLE" ]; then
    echo "📝 Створення IAM role..."
    aws iam create-role \
        --role-name "$ROLE_NAME" \
        --assume-role-policy-document file:///tmp/trust-policy.json \
        --output text > /dev/null 2>&1
    echo "✅ Role створено: $ROLE_NAME"
    
    # Прив'язати policy до role
    echo "📝 Прив'язка policy до role..."
    aws iam attach-role-policy \
        --role-name "$ROLE_NAME" \
        --policy-arn "$POLICY_ARN" \
        --output text > /dev/null 2>&1
    echo "✅ Policy прив'язано"
else
    echo "✅ Role вже існує: $ROLE_NAME"
fi

# Оновити addon з service account role
echo ""
echo "📋 Оновлення addon з IAM role..."
ROLE_ARN=$(aws iam get-role --role-name "$ROLE_NAME" --query 'Role.Arn' --output text 2>&1)

aws eks update-addon \
    --cluster-name $CLUSTER_NAME \
    --addon-name aws-ebs-csi-driver \
    --service-account-role-arn "$ROLE_ARN" \
    --region $REGION \
    --output text > /dev/null 2>&1

echo "✅ Addon оновлено з IAM role: $ROLE_ARN"

# Очистити тимчасові файли
rm -f /tmp/ebs-csi-driver-policy.json /tmp/trust-policy.json

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ НАЛАШТУВАННЯ ЗАВЕРШЕНО"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 НАСТУПНІ КРОКИ:"
echo ""
echo "1. Дочекатися, поки addon завершить встановлення (2-5 хвилин):"
echo "   aws eks describe-addon --cluster-name $CLUSTER_NAME --addon-name aws-ebs-csi-driver --region $REGION --query 'addon.status'"
echo ""
echo "2. Перевірити, чи запущені поди CSI driver:"
echo "   kubectl get pods -n kube-system | grep ebs-csi"
echo ""
echo "3. Перевірити PVC та поди:"
echo "   kubectl get pvc -n bank-system"
echo "   kubectl get pods -n bank-system"
echo ""
echo "4. Якщо все працює, PVC мають стати Bound протягом 1-2 хвилин"

