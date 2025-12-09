"""
org.dqman.Main module for AI confidence scoring using a seq-to-seq transformer AI
"""
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
import torch

if __name__ == "__main__":
    model_name = "Helsinki-NLP/opus-mt-en-de"
    model = AutoModelForSeq2SeqLM.from_pretrained(model_name)
    tokenizer = AutoTokenizer.from_pretrained(model_name)

    texts = ["The bank is closed today.", "The elderly woman's sycophantic praise for the king's pulchritudinous visage was a verisimilitudinous performance of accismus, as she secretly harbored a profound sense of lypophrenia."]

    for text in texts:
        inputs = tokenizer(text, return_tensors="pt")
        # Generate with output_scores to get logits for each token
        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                output_scores=True,
                return_dict_in_generate=True,
                max_length=50
            )

        # Extract probabilities from scores
        scores = outputs.scores  # Tuple of logit tensors
        probabilities = []

        for score in scores:
            # Convert logits to probabilities
            probs = torch.softmax(score, dim=-1)
            # Get the max probability for the selected token
            max_prob = probs.max().item()
            probabilities.append(max_prob)

        # Get the translated text
        translated = tokenizer.decode(outputs.sequences[0], skip_special_tokens=True)

        # Calculate average confidence
        avg_confidence = sum(probabilities) / len(probabilities) if probabilities else 0
        min_confidence = min(probabilities) if probabilities else 0

        print(f"Input: {text}")
        print(f"Translation: {translated}")
        print(f"Token probabilities: {[f'{p:.3f}' for p in probabilities]}")
        print(f"Average confidence: {avg_confidence:.1%}")
        print(f"minimum confidence: {min_confidence:.1%}")