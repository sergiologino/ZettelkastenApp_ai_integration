-- Remove Pollinations as an available provider and fallback target.

DELETE FROM client_network_access
WHERE neural_network_id IN (
    SELECT id
    FROM neural_networks
    WHERE lower(provider) = 'pollinations'
       OR lower(name) LIKE '%pollinations%'
       OR lower(coalesce(api_url, '')) LIKE '%pollinations%'
);

DELETE FROM network_limits
WHERE neural_network_id IN (
    SELECT id
    FROM neural_networks
    WHERE lower(provider) = 'pollinations'
       OR lower(name) LIKE '%pollinations%'
       OR lower(coalesce(api_url, '')) LIKE '%pollinations%'
);

DELETE FROM usage_counters
WHERE neural_network_id IN (
    SELECT id
    FROM neural_networks
    WHERE lower(provider) = 'pollinations'
       OR lower(name) LIKE '%pollinations%'
       OR lower(coalesce(api_url, '')) LIKE '%pollinations%'
);

DELETE FROM neural_networks
WHERE lower(provider) = 'pollinations'
   OR lower(name) LIKE '%pollinations%'
   OR lower(coalesce(api_url, '')) LIKE '%pollinations%';
