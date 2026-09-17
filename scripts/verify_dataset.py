import re
import os
import sys

def verify():
    data_sql_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)),
        "speakup-server", "src", "main", "resources", "data.sql"
    )

    assert os.path.exists(data_sql_path), f"data.sql does not exist at {data_sql_path}"

    with open(data_sql_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Match rows in prompts: (id, 'text', category_id, 'OFF_THE_CUFF', true, NOW())
    pattern = re.compile(r"\((\d+),\s*'([^']+)',\s*(\d+),\s*'([A-Z_]+)',\s*(true|false),\s*NOW\(\)\)")
    matches = pattern.findall(content)

    print(f"Total prompt rows parsed from data.sql: {len(matches)}")

    # 1. Total OFF_THE_CUFF prompts = 2,000
    off_the_cuff_prompts = [m for m in matches if m[3] == "OFF_THE_CUFF"]
    assert len(off_the_cuff_prompts) == 2000, f"Expected 2000 OFF_THE_CUFF prompts, found {len(off_the_cuff_prompts)}"
    print("[PASS] 1. Total OFF_THE_CUFF prompts = 2,000")

    # 2. All prompts are unique
    seen_texts = set()
    for prompt_id, text, cat_id, mode, active in off_the_cuff_prompts:
        key = text.strip().lower()
        assert key not in seen_texts, f"Duplicate prompt found: '{text}' (ID: {prompt_id})"
        seen_texts.add(key)
    assert len(seen_texts) == 2000, f"Expected 2000 unique prompts, found {len(seen_texts)}"
    print("[PASS] 2. All prompts are unique (0 duplicates)")

    # 3. Every prompt is 1–2 words (no questions, no sentences)
    for prompt_id, text, cat_id, mode, active in off_the_cuff_prompts:
        words = text.strip().split()
        assert 1 <= len(words) <= 2, f"Prompt ID {prompt_id} ('{text}') has {len(words)} words, expected 1 or 2"
        assert "?" not in text, f"Prompt ID {prompt_id} contains question mark: '{text}'"
        assert "!" not in text, f"Prompt ID {prompt_id} contains exclamation: '{text}'"
        assert "." not in text, f"Prompt ID {prompt_id} contains period: '{text}'"
    print("[PASS] 3. Every prompt is 1–2 words (no questions or sentences)")

    # 4. All prompts have a valid category
    category_counts = {}
    valid_categories = set(range(1, 12))
    for prompt_id, text, cat_id_str, mode, active in off_the_cuff_prompts:
        cat_id = int(cat_id_str)
        assert cat_id in valid_categories, f"Prompt ID {prompt_id} has invalid category {cat_id}"
        category_counts[cat_id] = category_counts.get(cat_id, 0) + 1

    print("[PASS] 4. All prompts have a valid category (1–11)")
    print("Category breakdown:")
    cat_names = {
        1: "Technology", 2: "Society", 3: "Mindset", 4: "Life", 5: "Career",
        6: "Education", 7: "Future", 8: "Environment", 9: "Science", 10: "People", 11: "Creativity"
    }
    for cat_id in sorted(category_counts.keys()):
        print(f"   Category {cat_id:2d} ({cat_names[cat_id]:12s}): {category_counts[cat_id]} topics")

    print("\nALL DATA VERIFICATIONS PASSED SUCCESSFULLY!")

if __name__ == "__main__":
    verify()
