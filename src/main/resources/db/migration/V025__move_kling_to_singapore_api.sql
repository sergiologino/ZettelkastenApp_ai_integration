-- Kling moved the international API from api.klingai.com to api-singapore.klingai.com.
UPDATE neural_networks
SET api_url = 'https://api-singapore.klingai.com',
    updated_at = NOW()
WHERE LOWER(provider) = 'kling'
  AND (
      api_url IS NULL
      OR TRIM(api_url) = ''
      OR LOWER(TRIM(TRAILING '/' FROM api_url)) = 'https://api.klingai.com'
      OR LOWER(TRIM(api_url)) LIKE 'https://api.klingai.com/%'
  );
