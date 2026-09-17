import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

from cat_tech_society import TECHNOLOGY, SOCIETY
from cat_mindset_life import MINDSET, LIFE
from cat_career_edu import CAREER, EDUCATION
from cat_future_env import FUTURE, ENVIRONMENT
from cat_science_people_creativity import SCIENCE, PEOPLE, CREATIVITY

CATEGORIES_DEF = [
    (1,  'Technology',   'Tech, AI, software, and digital culture',    TECHNOLOGY,  182),
    (2,  'Society',      'Social issues, culture, and human behavior', SOCIETY,     182),
    (3,  'Mindset',      'Psychology, habits, and personal growth',    MINDSET,     182),
    (4,  'Life',         'Everyday life, relationships, and meaning',  LIFE,        182),
    (5,  'Career',       'Work, ambition, and professional growth',    CAREER,      182),
    (6,  'Education',    'Learning, teaching, and knowledge',          EDUCATION,   182),
    (7,  'Future',       'What lies ahead for humanity and you',       FUTURE,      182),
    (8,  'Environment',  'Nature, climate, and sustainability',        ENVIRONMENT, 182),
    (9,  'Science',      'Discovery, research, and the unknown',       SCIENCE,     182),
    (10, 'People',       'Leaders, thinkers, and everyday heroes',     PEOPLE,      181),
    (11, 'Creativity',   'Art, imagination, and original thinking',    CREATIVITY,  181),
]

def build():
    all_prompts = []
    seen_prompts = set()

    for cat_id, cat_name, cat_desc, topics, target in CATEGORIES_DEF:
        selected = topics[:target]
        assert len(selected) == target, f"Category {cat_name} has {len(selected)} items, expected {target}"
        
        for topic in selected:
            clean_topic = topic.strip()
            words = clean_topic.split()
            # Requirement checks
            assert 1 <= len(words) <= 2, f"Word count invalid ({len(words)}) for '{clean_topic}'"
            assert '?' not in clean_topic, f"Question mark in '{clean_topic}'"
            assert '!' not in clean_topic, f"Exclamation in '{clean_topic}'"
            assert '.' not in clean_topic, f"Period in '{clean_topic}'"
            assert clean_topic[-1] not in ':;,', f"Trailing punctuation in '{clean_topic}'"
            
            key = clean_topic.lower()
            assert key not in seen_prompts, f"Duplicate detected: '{clean_topic}'"
            seen_prompts.add(key)
            
            all_prompts.append({
                "text": clean_topic,
                "category_id": cat_id,
                "category_name": cat_name,
                "mode": "OFF_THE_CUFF",
                "active": True
            })

    assert len(all_prompts) == 2000, f"Expected 2000 prompts, got {len(all_prompts)}"
    assert len(seen_prompts) == 2000, f"Expected 2000 unique prompts, got {len(seen_prompts)}"

    print(f"Validated {len(all_prompts)} unique OFF_THE_CUFF prompts across {len(CATEGORIES_DEF)} categories.")

    # Generate SQL
    lines = []
    lines.append("-- =============================================")
    lines.append("-- SpeakUp Seed Data - 2,000 Off The Cuff Topics")
    lines.append("-- =============================================")
    lines.append("")
    lines.append("-- Categories")
    lines.append("INSERT IGNORE INTO categories (id, name, description, created_at) VALUES")
    cat_values = []
    for cat_id, cat_name, cat_desc, _, _ in CATEGORIES_DEF:
        cat_values.append(f"({cat_id}, '{cat_name}', '{cat_desc}', NOW())")
    lines.append(",\n".join(cat_values) + ";")
    lines.append("")
    lines.append("-- Replace existing OFF_THE_CUFF dataset with exactly 2,000 curated topics")
    lines.append("DELETE FROM prompts WHERE mode = 'OFF_THE_CUFF';")
    lines.append("")

    # Chunk prompts by 100
    chunk_size = 100
    for chunk_idx in range(0, len(all_prompts), chunk_size):
        chunk = all_prompts[chunk_idx:chunk_idx + chunk_size]
        start_id = chunk_idx + 1
        end_id = chunk_idx + len(chunk)
        lines.append(f"-- Off the Cuff Prompts ({start_id} to {end_id})")
        lines.append("INSERT INTO prompts (id, text, category_id, mode, active, created_at) VALUES")
        row_values = []
        for offset, p in enumerate(chunk):
            prompt_id = chunk_idx + offset + 1
            escaped_text = p["text"].replace("'", "''")
            row_values.append(f"({prompt_id}, '{escaped_text}', {p['category_id']}, '{p['mode']}', true, NOW())")
        lines.append(",\n".join(row_values) + ";")
        lines.append("")

    output_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)),
        "speakup-server", "src", "main", "resources", "data.sql"
    )

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"Successfully generated {output_path} with {len(all_prompts)} prompts.")

if __name__ == "__main__":
    build()
