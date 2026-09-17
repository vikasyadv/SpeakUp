import re
from cat_tech_society import TECHNOLOGY, SOCIETY
from cat_mindset_life import MINDSET, LIFE
from cat_career_edu import CAREER, EDUCATION
from cat_future_env import FUTURE, ENVIRONMENT
from cat_science_people_creativity import SCIENCE, PEOPLE, CREATIVITY

raw_categories = {
    1: ("Technology", TECHNOLOGY, 182),
    2: ("Society", SOCIETY, 182),
    3: ("Mindset", MINDSET, 182),
    4: ("Life", LIFE, 182),
    5: ("Career", CAREER, 182),
    6: ("Education", EDUCATION, 182),
    7: ("Future", FUTURE, 182),
    8: ("Environment", ENVIRONMENT, 182),
    9: ("Science", SCIENCE, 182),
    10: ("People", PEOPLE, 181),
    11: ("Creativity", CREATIVITY, 181),
}

global_seen = set()
invalid_word_count = []
has_tags = []

for cat_id, (name, lst, target) in raw_categories.items():
    cleaned_list = []
    for item in lst:
        item_clean = item.strip()
        if "-" in item_clean:
            # check if it's a suffix tag like -Ppl, -Sci, etc.
            if re.search(r'-(Sci|Env|Fut|Edu|Ppl|Cre|Life)$', item_clean):
                has_tags.append((cat_id, name, item_clean))
        words = item_clean.split()
        if len(words) < 1 or len(words) > 2:
            invalid_word_count.append((cat_id, name, item_clean, len(words)))

print(f"Has tags count: {len(has_tags)}")
for t in has_tags:
    print("  Tag:", t)

print(f"Invalid word count: {len(invalid_word_count)}")
for inv in invalid_word_count:
    print("  Word count != 1 or 2:", inv)
