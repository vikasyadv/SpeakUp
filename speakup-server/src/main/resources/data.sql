-- =============================================
-- SpeakUp Seed Data
-- =============================================

-- Categories
INSERT IGNORE INTO categories (id, name, description, created_at) VALUES
(1,  'Technology',   'Tech, AI, software, and digital culture',      NOW()),
(2,  'Society',      'Social issues, culture, and human behavior',   NOW()),
(3,  'Mindset',      'Psychology, habits, and personal growth',      NOW()),
(4,  'Life',         'Everyday life, relationships, and meaning',    NOW()),
(5,  'Career',       'Work, ambition, and professional growth',      NOW()),
(6,  'Education',    'Learning, teaching, and knowledge',            NOW()),
(7,  'Future',       'What lies ahead for humanity and you',         NOW()),
(8,  'Environment',  'Nature, climate, and sustainability',          NOW()),
(9,  'Science',      'Discovery, research, and the unknown',         NOW()),
(10, 'People',       'Leaders, thinkers, and everyday heroes',       NOW()),
(11, 'Creativity',   'Art, imagination, and original thinking',      NOW());

-- Off the Cuff Prompts: Technology
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(1,  'Should social media platforms be regulated like public utilities?', 1, 'OFF_THE_CUFF', true, NOW()),
(2,  'Is artificial intelligence a bigger threat or opportunity for humanity?', 1, 'OFF_THE_CUFF', true, NOW()),
(3,  'Will physical cash disappear within your lifetime?', 1, 'OFF_THE_CUFF', true, NOW()),
(4,  'Should children under 13 be banned from using smartphones?', 1, 'OFF_THE_CUFF', true, NOW()),
(5,  'What is the most overrated piece of technology today?', 1, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Society
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(6,  'Is cancel culture a force for accountability or mob justice?',  2, 'OFF_THE_CUFF', true, NOW()),
(7,  'Should voting be mandatory in a democracy?',                   2, 'OFF_THE_CUFF', true, NOW()),
(8,  'Do people today value convenience more than freedom?',         2, 'OFF_THE_CUFF', true, NOW()),
(9,  'Is the idea of a perfect society achievable or dangerous?',    2, 'OFF_THE_CUFF', true, NOW()),
(10, 'Should billionaires exist?',                                   2, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Mindset
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(11, 'Is discipline more important than motivation?',                3, 'OFF_THE_CUFF', true, NOW()),
(12, 'What does it mean to be truly confident?',                     3, 'OFF_THE_CUFF', true, NOW()),
(13, 'Is failure necessary for success?',                            3, 'OFF_THE_CUFF', true, NOW()),
(14, 'Should people optimize every aspect of their lives?',          3, 'OFF_THE_CUFF', true, NOW()),
(15, 'Is overthinking the biggest obstacle to happiness?',           3, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Life
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(16, 'What does a meaningful life look like to you?',                4, 'OFF_THE_CUFF', true, NOW()),
(17, 'Is it better to have a wide circle of friends or a small close one?', 4, 'OFF_THE_CUFF', true, NOW()),
(18, 'What is one thing people waste the most time on?',             4, 'OFF_THE_CUFF', true, NOW()),
(19, 'Should people follow their passion or be practical?',          4, 'OFF_THE_CUFF', true, NOW()),
(20, 'Is it possible to be truly selfless?',                         4, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Career
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(21, 'Is remote work better than working in an office?',             5, 'OFF_THE_CUFF', true, NOW()),
(22, 'Should people stay loyal to one company or job-hop?',          5, 'OFF_THE_CUFF', true, NOW()),
(23, 'What matters more in a career: money or fulfillment?',         5, 'OFF_THE_CUFF', true, NOW()),
(24, 'Is a college degree still worth the investment?',              5, 'OFF_THE_CUFF', true, NOW()),
(25, 'What is the most underrated professional skill?',              5, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Education
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(26, 'Should schools teach financial literacy as a core subject?',   6, 'OFF_THE_CUFF', true, NOW()),
(27, 'Is homework actually beneficial for students?',                6, 'OFF_THE_CUFF', true, NOW()),
(28, 'Should standardized testing be abolished?',                    6, 'OFF_THE_CUFF', true, NOW()),
(29, 'Is self-education more valuable than formal education?',       6, 'OFF_THE_CUFF', true, NOW()),
(30, 'What is the one subject every school should teach but does not?', 6, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Future
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(31, 'Will humans colonize another planet in this century?',         7, 'OFF_THE_CUFF', true, NOW()),
(32, 'What will the biggest invention of the next 20 years be?',     7, 'OFF_THE_CUFF', true, NOW()),
(33, 'Will robots replace most human jobs?',                         7, 'OFF_THE_CUFF', true, NOW()),
(34, 'Is immortality something humans should pursue?',               7, 'OFF_THE_CUFF', true, NOW()),
(35, 'What does the ideal city of 2050 look like?',                  7, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Environment
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(36, 'Is individual action enough to fight climate change?',         8, 'OFF_THE_CUFF', true, NOW()),
(37, 'Should governments ban single-use plastics entirely?',         8, 'OFF_THE_CUFF', true, NOW()),
(38, 'Is nuclear energy the answer to the climate crisis?',          8, 'OFF_THE_CUFF', true, NOW()),
(39, 'Should meat consumption be taxed for environmental reasons?',  8, 'OFF_THE_CUFF', true, NOW()),
(40, 'Can technology solve the problems technology created?',        8, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Science
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(41, 'Should there be limits on genetic engineering in humans?',     9, 'OFF_THE_CUFF', true, NOW()),
(42, 'Is the universe infinite, and does it matter?',                9, 'OFF_THE_CUFF', true, NOW()),
(43, 'What scientific discovery would change everything?',           9, 'OFF_THE_CUFF', true, NOW()),
(44, 'Should we fear artificial general intelligence?',              9, 'OFF_THE_CUFF', true, NOW()),
(45, 'Is time travel theoretically possible?',                       9, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: People
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(46, 'Who is the most influential person alive today and why?',      10, 'OFF_THE_CUFF', true, NOW()),
(47, 'What makes someone a great leader?',                           10, 'OFF_THE_CUFF', true, NOW()),
(48, 'Should we separate the art from the artist?',                  10, 'OFF_THE_CUFF', true, NOW()),
(49, 'Is it better to be feared or respected?',                      10, 'OFF_THE_CUFF', true, NOW()),
(50, 'What can we learn from people we disagree with?',              10, 'OFF_THE_CUFF', true, NOW());

-- Off the Cuff Prompts: Creativity
INSERT IGNORE INTO prompts (id, text, category_id, mode, active, created_at) VALUES
(51, 'Is creativity something you are born with or something you develop?', 11, 'OFF_THE_CUFF', true, NOW()),
(52, 'Will AI ever create art that is as meaningful as human art?',         11, 'OFF_THE_CUFF', true, NOW()),
(53, 'What is the most creative solution you have ever seen to a problem?', 11, 'OFF_THE_CUFF', true, NOW()),
(54, 'Is originality dead in the age of the internet?',                     11, 'OFF_THE_CUFF', true, NOW()),
(55, 'Should art be funded by governments?',                                11, 'OFF_THE_CUFF', true, NOW());
