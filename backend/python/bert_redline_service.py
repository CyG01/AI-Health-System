"""
Layer2 BERT 医疗红线判别模型服务（Python/Flask 微服务）

功能：
- 加载预训练的医疗领域 BERT 模型
- 对用户输入进行二分类：safe / unsafe
- 覆盖高危场景：自杀倾向、药品滥用、虚假诊疗、偏方风险
- 目标：拦截率 >= 99.9%（0.1% 漏检）

启动方式：
    python bert_redline_service.py --port 5001

依赖：
    pip install flask transformers torch
"""

import argparse
import logging
import json
import os
import time
from dataclasses import dataclass
from typing import Optional

import torch
from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForSequenceClassification

# ===================== 配置 =====================

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)

# 模型路径（优先使用本地路径，否则从 HuggingFace 下载）
MODEL_PATH = os.environ.get('BERT_REDLINE_MODEL_PATH', 'bert-base-chinese')

# 分类标签
LABELS = ['safe', 'unsafe']

# 拦截阈值：softmax 后 unsafe 分数 >= THRESHOLD 即判定为不安全
THRESHOLD = float(os.environ.get('BERT_REDLINE_THRESHOLD', '0.85'))

# 最大输入长度
MAX_LENGTH = 512

# ===================== 数据模型 =====================

@dataclass
class ClassifyResult:
    label: str
    confidence: float
    safe: bool
    elapsed_ms: float

# ===================== 模型加载 =====================

def load_model():
    """加载 BERT 模型和分词器。"""
    logger.info(f"正在加载 BERT 模型: {MODEL_PATH}")
    t0 = time.time()
    tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
    model = AutoModelForSequenceClassification.from_pretrained(
        MODEL_PATH,
        num_labels=2,
        ignore_mismatched_sizes=True
    )
    model.eval()
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = model.to(device)
    logger.info(f"模型加载完成，耗时 {time.time() - t0:.1f}s，设备: {device}")
    return model, tokenizer, device

# ===================== 医疗红线检测语料库（用于 Few-shot 增强） =====================

# 高危类别及关键词（用于规则增强，补充 BERT 判断）
HIGH_RISK_PATTERNS = {
    'suicide': [
        '不想活', '自杀', '结束生命', '活下去没意思', '一了百了',
        '怎么死', '安眠药', '跳楼', '割腕', '轻生'
    ],
    'drug_abuse': [
        '处方药', '止痛药上瘾', '安眠药成瘾', '违规用药', '滥用药物',
        '精神类药物', '依赖性', '自行加大剂量', '私自用药'
    ],
    'fake_treatment': [
        '偏方', '秘方', '土方', '包治百病', '祖传秘方', '特效药',
        '保证治愈', '疗程', '根治', '药到病除'
    ],
    'misleading_advice': [
        '停药', '不看医生', '别去医院', '不用检查', '自己治',
        '不用打疫苗', '药没用', '医院骗钱', '医生坑人'
    ]
}

def rule_based_check(text: str) -> Optional[bool]:
    """
    规则快速匹配：如果命中高危关键词，直接返回 unsafe。
    补充 BERT 模型可能在边缘 case 上的漏判。
    """
    text_lower = text.lower()
    for category, patterns in HIGH_RISK_PATTERNS.items():
        for pattern in patterns:
            if pattern in text_lower:
                return False  # unsafe
    return None  # 需要 BERT 判断

# ===================== 核心推理 =====================

def classify_text(
    text: str,
    model,
    tokenizer,
    device: torch.device
) -> ClassifyResult:
    """
    对输入文本进行二分类。
    
    Args:
        text: 用户输入文本
        
    Returns:
        ClassifyResult: 分类结果
    """
    t0 = time.time()

    # Step 1: 规则快速拦截
    rule_result = rule_based_check(text)
    if rule_result is False:
        elapsed = (time.time() - t0) * 1000
        return ClassifyResult(
            label='unsafe',
            confidence=1.0,
            safe=False,
            elapsed_ms=elapsed
        )

    # Step 2: BERT 推理
    inputs = tokenizer(
        text,
        return_tensors='pt',
        max_length=MAX_LENGTH,
        truncation=True,
        padding=True
    ).to(device)

    with torch.no_grad():
        outputs = model(**inputs)
        logits = outputs.logits
        probs = torch.softmax(logits, dim=-1)
        scores = probs[0].cpu().numpy()

    unsafe_score = float(scores[1])
    safe_score = float(scores[0])
    is_safe = unsafe_score < THRESHOLD

    elapsed = (time.time() - t0) * 1000
    
    # 翻转逻辑：如果不安全分数偏高则 unsafe
    if unsafe_score >= THRESHOLD:
        label = 'unsafe'
        confidence = unsafe_score
    else:
        label = 'safe'
        confidence = safe_score

    return ClassifyResult(
        label=label,
        confidence=confidence,
        safe=is_safe,
        elapsed_ms=elapsed
    )

# ===================== Flask 服务 =====================

app = Flask(__name__)

# 全局模型实例
_model = None
_tokenizer = None
_device = None

@app.before_first_request
def init_model():
    global _model, _tokenizer, _device
    _model, _tokenizer, _device = load_model()

@app.route('/health', methods=['GET'])
def health():
    """健康检查。"""
    return jsonify({'status': 'ok', 'model': MODEL_PATH})

@app.route('/v1/classify', methods=['POST'])
def classify():
    """
    对文本进行医疗红线判别。
    
    Body:
        {"text": "..."}
    
    Response:
        {
            "safe": true/false,
            "label": "safe" | "unsafe",
            "confidence": 0.9876,
            "elapsed_ms": 12.3
        }
    """
    try:
        data = request.get_json(force=True)
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'text is empty'}), 400
        
        if len(text) > MAX_LENGTH * 10:
            return jsonify({'error': f'text too long, max {MAX_LENGTH * 10} chars'}), 413

        result = classify_text(text, _model, _tokenizer, _device)

        return jsonify({
            'safe': result.safe,
            'label': result.label,
            'confidence': round(result.confidence, 4),
            'elapsed_ms': round(result.elapsed_ms, 2)
        })

    except Exception as e:
        logger.error(f"分类异常: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500

@app.route('/v1/batch_classify', methods=['POST'])
def batch_classify():
    """批量分类。"""
    try:
        data = request.get_json(force=True)
        texts = data.get('texts', [])

        if not texts or len(texts) > 100:
            return jsonify({'error': 'texts must be 1-100 items'}), 400

        results = []
        for text in texts:
            result = classify_text(text.strip(), _model, _tokenizer, _device)
            results.append({
                'text': text[:100],
                'safe': result.safe,
                'label': result.label,
                'confidence': round(result.confidence, 4),
                'elapsed_ms': round(result.elapsed_ms, 2)
            })

        unsafe_count = sum(1 for r in results if not r['safe'])
        return jsonify({
            'total': len(results),
            'unsafe_count': unsafe_count,
            'results': results
        })

    except Exception as e:
        logger.error(f"批量分类异常: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500

# ===================== 模型微调脚本（独立运行） =====================

def fine_tune_script():
    """
    微调 BERT 模型用于医疗红线检测。

    使用方式：
        python bert_redline_service.py --fine_tune --train_data ./redline_train.jsonl --epochs 3

    训练数据格式（JSONL）：
        {"text": "...", "label": 0}  # 0=safe, 1=unsafe
    """
    import argparse as _argparse
    from datasets import Dataset
    from transformers import TrainingArguments, Trainer

    parser = _argparse.ArgumentParser()
    parser.add_argument('--train_data', required=True, help='训练数据路径 (JSONL)')
    parser.add_argument('--epochs', type=int, default=3)
    parser.add_argument('--batch_size', type=int, default=8)
    parser.add_argument('--output_dir', default='./bert_redline_finetuned')
    args = parser.parse_args()

    # 加载数据
    texts, labels = [], []
    with open(args.train_data, 'r', encoding='utf-8') as f:
        for line in f:
            item = json.loads(line)
            texts.append(item['text'])
            labels.append(int(item['label']))

    dataset = Dataset.from_dict({'text': texts, 'label': labels})
    dataset = dataset.train_test_split(test_size=0.1, seed=42)

    # 加载模型
    tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL_PATH, num_labels=2)

    def tokenize_fn(examples):
        return tokenizer(examples['text'], truncation=True, padding='max_length', max_length=MAX_LENGTH)

    tokenized_train = dataset['train'].map(tokenize_fn, batched=True)
    tokenized_eval = dataset['test'].map(tokenize_fn, batched=True)

    training_args = TrainingArguments(
        output_dir=args.output_dir,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        evaluation_strategy='epoch',
        save_strategy='epoch',
        logging_dir='./logs',
        load_best_model_at_end=True,
        metric_for_best_model='eval_loss'
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=tokenized_train,
        eval_dataset=tokenized_eval,
        tokenizer=tokenizer
    )

    trainer.train()

    # 保存微调后的模型
    model.save_pretrained(args.output_dir)
    tokenizer.save_pretrained(args.output_dir)
    logger.info(f"微调完成，模型保存到 {args.output_dir}")

# ===================== 入口 =====================

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='BERT 医疗红线判别服务')
    parser.add_argument('--port', type=int, default=5001, help='服务端口')
    parser.add_argument('--host', default='0.0.0.0', help='服务地址')
    parser.add_argument('--fine_tune', action='store_true', help='运行微调脚本')
    parser.add_argument('--train_data', help='训练数据路径 (微调模式)')
    parser.add_argument('--epochs', type=int, default=3, help='微调轮数')
    args = parser.parse_args()

    if args.fine_tune:
        fine_tune_script()
    else:
        logger.info(f"启动 BERT 医疗红线判别服务 on {args.host}:{args.port}")
        app.run(host=args.host, port=args.port, debug=False)