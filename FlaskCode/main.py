from flask import Flask, jsonify, request
import snowflake.connector
import json
from datetime import datetime, timedelta

import pytz
import requests

app = Flask(__name__)
conn = snowflake.connector.connect(
    user='XXXXXXXX',
    password='XXXXXXX',
    account='XXXXXXXXXXX',
    database='PAY_ZONE',
    warehouse='COMPUTE_WH',
    schema='PAY_ZONE_SCHEMA',
    role='SYSADMIN'
)
cur = conn.cursor()

def execute_snowflake(query):
    cur.execute("USE DATABASE PAY_ZONE")
    cur.execute("USE SCHEMA PAY_ZONE_SCHEMA")
    cur.execute("USE WAREHOUSE COMPUTE_WH")
    cur.execute(query)
    conn.commit()
    result = cur.fetchall()
    return result

def insert_snowflake(sql, json_data):
    # Connect to Snowflake
    cur.execute("USE DATABASE PAY_ZONE")
    cur.execute("USE SCHEMA PAY_ZONE_SCHEMA")
    cur.execute("USE WAREHOUSE COMPUTE_WH")
    # Execute the SQL statement
    cur.execute(sql, json_data)
    # Commit the transaction
    conn.commit()
    
def transform_contract_data(input_json):
    # Convert date from DD/MM/YYYY to YYYY-MM-DD
    from_date = datetime.strptime(input_json['from_date'], '%d/%m/%Y').strftime('%Y-%m-%d')
    to_date = datetime.strptime(input_json['to_date'], '%d/%m/%Y').strftime('%Y-%m-%d')
    
    # Convert time to TIME format
    from_time = datetime.strptime(input_json['from_time'], '%H:%M').strftime('%H:%M:%S')
    to_time = datetime.strptime(input_json['to_time'], '%H:%M').strftime('%H:%M:%S')
    
    # Convert pay_rate to float
    hourly_pay = float(input_json['pay_rate'])
    payout_interval = input_json['payout_interval']
    if "minute" in payout_interval.lower():
        payout_interval = "minutely"
    elif "hour" in payout_interval.lower():
        payout_interval = "hourly"
    elif "day" in payout_interval.lower():
        payout_interval = "daily"
        
    # Prepare transformed data
    transformed_data = {
        'ISSUER_EMAIL': input_json['issuer_email'],
        'CONTRACTOR_EMAIL': input_json['contractor_email'],
        'HOURLY_PAY': hourly_pay,
        'STATUS': 'Pending Acceptance',
        'CONTRACT_START_DATE': from_date,
        'CONTRACT_END_DATE': to_date,
        'DAILY_START_TIME': from_time,
        'DAILY_END_TIME': to_time,
        'PAYOUT_INTERVAL': payout_interval
    }
    
    return transformed_data

from datetime import datetime, timedelta

def insert_snowflake(sql, json_data):
    cur.execute("USE DATABASE PAY_ZONE")
    cur.execute("USE SCHEMA PAY_ZONE_SCHEMA")
    cur.execute("USE WAREHOUSE COMPUTE_WH")
    cur.execute(sql, json_data)
    conn.commit()
    
def get_account_details(email_id):
    cur.execute("SELECT ACCOUNT_NUMBER FROM PAY_ZONE.PAY_ZONE_SCHEMA.ACCOUNT_TABLE WHERE EMAIL = %s", (email_id,))
    return cur.fetchone()[0]
    
def get_contract_details(contract_id):
    cur.execute("SELECT ISSUER_EMAIL, CONTRACTOR_EMAIL, HOURLY_PAY, PAYOUT_INTERVAL FROM PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE WHERE CONTRACT_ID = %s", (contract_id,))
    return cur.fetchone()

    return cur.fetchone()[0]
    
def get_work_units(contract_id,recent_transaction_time=None):
    if recent_transaction_time is None:
        cur.execute("SELECT COUNT(*) FROM PAY_ZONE.PAY_ZONE_SCHEMA.WORK_LOGS WHERE CONTRACT_ID = %s", (contract_id,))
    else:
        cur.execute("SELECT COUNT(*) FROM PAY_ZONE.PAY_ZONE_SCHEMA.WORK_LOGS WHERE CONTRACT_ID = %s and LOG_TIME > %s ", (contract_id,recent_transaction_time,))
    return cur.fetchone()[0]

def insert_transaction(debitor_account, creditor_account, amount, contract_id):
    sql = """
    INSERT INTO PAY_ZONE.PAY_ZONE_SCHEMA.TRANSACTIONS (DEBITOR_ACCOUNT, CREDITOR_ACCOUNT, AMOUNT, CONTRACT_ID) 
    VALUES (%s, %s, %s, %s)
    """
    data = (debitor_account, creditor_account, amount, contract_id)
    insert_snowflake(sql, data)

def get_most_recent_transaction_time(contract_id):
    cur.execute("SELECT MAX(TRANSACTION_TIME) FROM PAY_ZONE.PAY_ZONE_SCHEMA.TRANSACTIONS WHERE CONTRACT_ID = %s", (contract_id,))
    return cur.fetchone()[0]


def process_contracts(contract_ids):
    for contract_id in contract_ids:
        contract_details = get_contract_details(contract_id)
        if not contract_details:
            continue
        
        issuer_email, contractor_email, hourly_pay, payout_interval = contract_details
        recent_transaction_time = get_most_recent_transaction_time(contract_id)
        current_time = datetime.utcnow()
    
        work_units_pay = hourly_pay/60
        
        if payout_interval == 'minutely':
            min_payout_work_units = 1
        elif payout_interval == 'hourly':
            min_payout_work_units = 60
        elif payout_interval == 'daily':
            min_payout_work_units = 60*24
        else:
            continue
        current_work_units = get_work_units(contract_id,recent_transaction_time)
        if current_work_units >= min_payout_work_units:
            pay_ammount = current_work_units*work_units_pay
            debitor_account = get_account_details(issuer_email)
            creditor_account = get_account_details(contractor_email)
            insert_transaction(debitor_account, creditor_account, pay_ammount, contract_id)

# Function to check if point is inside polygon using ray-casting algorithm
def is_point_in_polygon(point, polygon):
    x, y = point
    n = len(polygon)
    inside = False

    p1x, p1y = polygon[0]
    for i in range(n + 1):
        p2x, p2y = polygon[i % n]
        if y > min(p1y, p2y):
            if y <= max(p1y, p2y):
                if x <= max(p1x, p2x):
                    if p1y != p2y:
                        xinters = (y - p1y) * (p2x - p1x) / (p2y - p1y) + p1x
                    if p1x == p2x or x <= xinters:
                        inside = not inside
        p1x, p1y = p2x, p2y

    return inside

def get_account_auth_token(psuUsername):
    META_INFO = {
        'clientId': 'x8iLINhlCad8vJaFdZYCpQuO-UdvLqmItGQQZ0VzdVo=',
        'clientSecret': 'F4QXWboJ_Ua9Ns1RV6BF-6kzmzRU5mSEB44G0lN4kyY=',
        'redirectUrl': 'https://d7006098-dc91-4117-bba7-e7eac79263cb.example.org/redirect',
        'psuUsername': str(psuUsername)+'@d7006098-dc91-4117-bba7-e7eac79263cb.example.org'
    }
    url = "https://api.sandbox.natwest.com/.well-known/openid-configuration"
    payload = {}
    headers = {}
    
    config_response = requests.request("GET", url, headers=headers, data=payload)
    configs_data = config_response.json()
    
    AUTHORIZATION_ENDPOINT = configs_data['authorization_endpoint']
    TOKEN_ENDPOINT = configs_data['token_endpoint']
    
    url = TOKEN_ENDPOINT
    payload = f"grant_type=client_credentials&client_id={META_INFO['clientId']}&client_secret={META_INFO['clientSecret']}&scope=accounts"
    headers = {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
    token_endpoint_response = requests.request("POST", url, headers=headers, data=payload)
    token_endpoint_data = token_endpoint_response.json()
    ACCOUNT_REQUEST_ACCESS_TOKEN = token_endpoint_data['access_token']
    
    url = "https://ob.sandbox.natwest.com/open-banking/v3.1/aisp/account-access-consents"
    payload = json.dumps({
      "Data": {
        "Permissions": [
          "ReadAccountsDetail",
          "ReadBalances",
          "ReadTransactionsCredits",
          "ReadTransactionsDebits",
          "ReadTransactionsDetail"
        ]
      },
      "Risk": {}
    })
    headers = {
      'Authorization': f'Bearer {ACCOUNT_REQUEST_ACCESS_TOKEN}',
      'Content-Type': 'application/json'
    }
    
    access_consent_response = requests.request("POST", url, headers=headers, data=payload)
    access_consent_response_data = access_consent_response.json()
    CONSENT_ID = access_consent_response_data['Data']['ConsentId']
    
    url = f"{AUTHORIZATION_ENDPOINT}?client_id={META_INFO['clientId']}&response_type=code id_token&scope=openid accounts&redirect_uri={META_INFO['redirectUrl']}&state=ABC&request={CONSENT_ID}&authorization_mode=AUTO_POSTMAN&authorization_username={META_INFO['psuUsername']}"
    payload = {}
    headers = {}
    auth_response = requests.request("GET", url, headers=headers, data=payload)
    auth_response_data = auth_response.json()
    auth_response_dict = {item.split("=")[0]:item.split("=")[1] for item in auth_response_data['redirectUri'].split('#')[-1].split("&")}
    AUTHORIZATION_CODE = auth_response_dict['code']
    
    url = "https://ob.sandbox.natwest.com/token"
    payload = f"client_id={META_INFO['clientId']}&client_secret={META_INFO['clientSecret']}&redirect_uri={META_INFO['redirectUrl']}&grant_type=authorization_code&code={AUTHORIZATION_CODE}"
    headers = {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
    access_token_response = requests.request("POST", url, headers=headers, data=payload)
    access_token_data = access_token_response.json()
    API_ACCESS_TOKEN = access_token_data['access_token']
    return API_ACCESS_TOKEN
    
@app.route('/api/add_user_info', methods=['POST'])
def handle_form_data():
    # Access form data
    name = request.form.get('name')
    email = request.form.get('email')
    uuid = request.form.get('uid')

    query = f"""MERGE INTO PAY_ZONE.PAY_ZONE_SCHEMA.USER_TABLE AS target
    USING (VALUES
        ('{email}', '{name}', '{uuid}')
    ) AS source (EMAIL, USER_NAME, UUID)
    ON target.EMAIL = source.EMAIL
    WHEN MATCHED THEN
        UPDATE SET
            target.USER_NAME = source.USER_NAME,
            target.UUID = source.UUID
    WHEN NOT MATCHED THEN
        INSERT (EMAIL, USER_NAME, UUID)
        VALUES (source.EMAIL, source.USER_NAME, source.UUID);"""
    result = execute_snowflake(query)
    print(result)

    response = jsonify({
        'status': 'success',
        'data': {}
    })
    
    return response
    
@app.route('/api/add_contract', methods=['POST'])
def handle_new_contract_data():
    # Access form data
    input_json = json.loads(request.data)
    print("-"*50)
    print(input_json)
    print("-"*50)
    transformed_data = transform_contract_data(input_json)
    
    # Prepare the SQL INSERT statement
    sql = """
    INSERT INTO PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE 
    (ISSUER_EMAIL, CONTRACTOR_EMAIL, HOURLY_PAY, STATUS, CONTRACT_START_DATE, CONTRACT_END_DATE, DAILY_START_TIME, DAILY_END_TIME, PAYOUT_INTERVAL)
    VALUES 
    (%(ISSUER_EMAIL)s, %(CONTRACTOR_EMAIL)s, %(HOURLY_PAY)s, %(STATUS)s, %(CONTRACT_START_DATE)s, %(CONTRACT_END_DATE)s, %(DAILY_START_TIME)s, %(DAILY_END_TIME)s, %(PAYOUT_INTERVAL)s)
    """
    insert_snowflake(sql,transformed_data)
    
    contract_id = execute_snowflake("SELECT CONTRACT_ID FROM PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE ORDER BY ISSUE_DATE DESC LIMIT 1;")[0][0]
    
    markers = input_json.get('markers', [])
    for marker in markers:
        print("INSERING MARKEER")
        print(marker)
        print("-")
        processed_json = {
            "LATITUDE": marker.get('latitude'),
            "LONGITUDE": marker.get('longitude'),
            "MARKER_IDX": marker.get('marker_idx'),
            "CONTRACT_ID": contract_id
        }
        insert_query = """
            INSERT INTO PAY_ZONE.PAY_ZONE_SCHEMA.ZONE_TABLE (CONTRACT_ID, LATITUDE, LONGITUDE, MARKER_IDX)
            VALUES (%(CONTRACT_ID)s, %(LATITUDE)s, %(LONGITUDE)s, %(MARKER_IDX)s)
        """
        insert_snowflake(insert_query,processed_json)

    response = jsonify({
        'status': 'success',
        'data': {}
    })
    
    return response

@app.route('/api/outbound_contracts_info', methods=['POST'])
def outbound_contracts_info():
    email = request.form.get('email')
    fetch_contracts = execute_snowflake(f"Select * from PAY_ZONE_SCHEMA.CONTRACT_TABLE WHERE ISSUER_EMAIL='{email}'")
    response = json.dumps(fetch_contracts,default=str)
    return response

@app.route('/api/contract_info', methods=['POST'])
def contract_info():
    contract_id = request.form.get('contract_id')
    fetch_contracts = execute_snowflake(f"Select * from PAY_ZONE_SCHEMA.CONTRACT_TABLE WHERE CONTRACT_ID={contract_id}")
    response = json.dumps(fetch_contracts,default=str)
    return response

@app.route('/api/inbound_contracts_info', methods=['POST'])
def inbound_contracts_info():
    email = request.form.get('email')
    fetch_contracts = execute_snowflake(f"Select * from PAY_ZONE_SCHEMA.CONTRACT_TABLE WHERE CONTRACTOR_EMAIL='{email}'")
    response = json.dumps(fetch_contracts,default=str)
    return response


@app.route('/api/zone_info_id', methods=['POST'])
def zone_id_contract_info():
    contract_id = request.form.get('contract_id')
    fetch_contracts = execute_snowflake(f"Select * from PAY_ZONE_SCHEMA.ZONE_TABLE WHERE CONTRACT_ID={contract_id}")
    response = json.dumps(fetch_contracts,default=str)
    return response

@app.route('/api/accept_contract_id', methods=['POST'])
def accept_contract_id():
    contract_id = request.form.get('contract_id')
    fetch_contracts = execute_snowflake(f"""UPDATE PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE
    SET STATUS = CASE 
                     WHEN CURRENT_DATE() BETWEEN CONTRACT_START_DATE AND CONTRACT_END_DATE THEN 'Active'
                     ELSE 'Accepted'
                  END
    WHERE CONTRACT_ID = {contract_id};""")
        
    response = json.dumps(fetch_contracts,default=str)
    return response

@app.route('/api/reject_contract_id', methods=['POST'])
def reject_contract_id():
    contract_id = request.form.get('contract_id')
    fetch_contracts = execute_snowflake(f"UPDATE PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE SET STATUS = 'Rejected' WHERE CONTRACT_ID = {contract_id}")
    response = json.dumps(fetch_contracts,default=str)
    return response
    
@app.route('/api/zone_info_email_contractor', methods=['POST'])
def zone_info_email_contractor():
    email = request.form.get('email')
    fetch_contracts = execute_snowflake(f"""SELECT z.CONTRACT_ID, z.LATITUDE, z.LONGITUDE, z.MARKER_IDX
FROM PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE c
JOIN PAY_ZONE.PAY_ZONE_SCHEMA.ZONE_TABLE z
ON c.CONTRACT_ID = z.CONTRACT_ID
WHERE c.CONTRACTOR_EMAIL = '{email}' and c.STATUS = 'Active';""")
    response = json.dumps(fetch_contracts,default=str)
    return response
    

@app.route('/api/zone_info_email_contractor_gps', methods=['POST'])
def zone_info_email_contractor_gps():
    email = request.form.get('email')
    latitude = float(request.form.get('latitude'))
    longitude = float(request.form.get('longitude'))

    # Fetch active contracts and zones
    fetch_contracts_query = f"""
    SELECT z.CONTRACT_ID, z.LATITUDE, z.LONGITUDE, z.MARKER_IDX
    FROM PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE c
    JOIN PAY_ZONE.PAY_ZONE_SCHEMA.ZONE_TABLE z
    ON c.CONTRACT_ID = z.CONTRACT_ID
    WHERE c.CONTRACTOR_EMAIL = '{email}' and c.STATUS = 'Active';
    """
    fetch_contracts = execute_snowflake(fetch_contracts_query)

    # Organize zone data by CONTRACT_ID
    zones = {}
    for contract in fetch_contracts:
        contract_id, lat, lon, marker_idx = contract
        if contract_id not in zones:
            zones[contract_id] = []
        zones[contract_id].append((lat, lon))

    # Check if user is inside any of the zones
    active_zones = []
    for contract_id, polygon in zones.items():
        if is_point_in_polygon((latitude, longitude), polygon):
            active_zones.append(contract_id)

    response = json.dumps(active_zones, default=str)
    return response
    
@app.route('/api/zone_info_email_issuer', methods=['POST'])
def zone_info_email_issuer():
    email = request.form.get('email')
    fetch_contracts = execute_snowflake(f"""SELECT z.CONTRACT_ID, z.LATITUDE, z.LONGITUDE, z.MARKER_IDX
FROM PAY_ZONE.PAY_ZONE_SCHEMA.CONTRACT_TABLE c
JOIN PAY_ZONE.PAY_ZONE_SCHEMA.ZONE_TABLE z
ON c.CONTRACT_ID = z.CONTRACT_ID
WHERE c.ISSUER_EMAIL = '{email}' and c.STATUS = 'Active';""")
    response = json.dumps(fetch_contracts,default=str)
    return response
    
@app.route('/api/insert_work_minute_logs', methods=['POST'])
def insert_work_minute_logs():
    contract_ids = request.form.get('contract_ids')
    contract_ids = [int(val.strip()) for val in contract_ids[1:-1].split(",") if val.strip()!=""]
    
    values_str = ', '.join([f"('{contract_id}')" for contract_id in contract_ids])
    query = f"INSERT INTO PAY_ZONE.PAY_ZONE_SCHEMA.WORK_LOGS (CONTRACT_ID) VALUES {values_str};"
    
    # Execute the query
    execute_snowflake(query)
    process_contracts(contract_ids)
    response = json.dumps({})
    return response

    
@app.route('/api/account_info', methods=['POST'])
def account_info():
    psuUsername = request.form.get('user_identification_number')
    API_ACCOUNT_ACCESS_TOKEN = get_account_auth_token(psuUsername)
    url = "https://ob.sandbox.natwest.com/open-banking/v3.1/aisp/accounts"
    payload = {}
    headers = {
      'Authorization': f'Bearer {API_ACCOUNT_ACCESS_TOKEN}'
    }
    account_info_response = requests.request("GET", url, headers=headers, data=payload)
    account_info_data = account_info_response.json()
    res_arr = []
    for account in account_info_data['Data']['Account']:
        key_info = {
            'AccountId': account['AccountId'],
            'AccountType': account['AccountType'],
            'AccountSubType': account['AccountSubType'],
            'AccountNum': account['Account'][0]['Identification'],
            'Name': account['Nickname']
        }
        res_arr.append(key_info)
    response = json.dumps(res_arr,default=str)
    return response
    
@app.route('/api/set_primary_account', methods=['POST'])
def set_primary_account():
    psuUsername = request.form.get('user_identification_number')
    email = request.form.get('email')
    account_id = request.form.get('account_id')
    account_name = request.form.get('account_name')
    account_number = request.form.get('account_number')
    account_type = request.form.get('account_type')
    account_sub_type = request.form.get('account_sub_type')

    
    API_ACCOUNT_ACCESS_TOKEN = get_account_auth_token(psuUsername)
    url = f"https://ob.sandbox.natwest.com/open-banking/v3.1/aisp/accounts/{account_id}/balances"
    payload = {}
    headers = {
      'Authorization': f'Bearer {API_ACCOUNT_ACCESS_TOKEN}'
    }
    balance_request = requests.request("GET", url, headers=headers, data=payload)
    balance_data = balance_request.json()
    balance = [val for val in balance_data['Data']['Balance'] if val['Type']=='ForwardAvailable'][0]['Amount']['Amount']
    # TODO CONTINUE WITH ADDING ACC INFO WIHT BALANCE


    fetch_contracts = execute_snowflake(f"""MERGE INTO PAY_ZONE.PAY_ZONE_SCHEMA.ACCOUNT_TABLE AS target
USING (
    SELECT
        '{email}' AS EMAIL, 
        '{account_id}' AS ACCOUNT_ID, 
        '{account_name}' AS ACCOUNT_NAME, 
        '{account_number}' AS ACCOUNT_NUMBER, 
        '{account_type}' AS ACCOUNT_TYPE, 
        '{account_sub_type}' AS ACCOUNT_SUB_TYPE, 
        {balance} AS ACCOUNT_BALANCE
) AS source
ON target.EMAIL = source.EMAIL
WHEN MATCHED THEN
    UPDATE SET 
        target.ACCOUNT_ID = source.ACCOUNT_ID,
        target.ACCOUNT_NAME = source.ACCOUNT_NAME,
        target.ACCOUNT_NUMBER = source.ACCOUNT_NUMBER,
        target.ACCOUNT_TYPE = source.ACCOUNT_TYPE,
        target.ACCOUNT_SUB_TYPE = source.ACCOUNT_SUB_TYPE,
        target.ACCOUNT_BALANCE = source.ACCOUNT_BALANCE
WHEN NOT MATCHED THEN
    INSERT (EMAIL, ACCOUNT_ID, ACCOUNT_NAME, ACCOUNT_NUMBER, ACCOUNT_TYPE, ACCOUNT_SUB_TYPE, ACCOUNT_BALANCE)
    VALUES (source.EMAIL, source.ACCOUNT_ID, source.ACCOUNT_NAME, source.ACCOUNT_NUMBER, source.ACCOUNT_TYPE, source.ACCOUNT_SUB_TYPE, source.ACCOUNT_BALANCE);""")
    response = json.dumps(fetch_contracts,default=str)
    return response
    
@app.route('/api/user_details', methods=['POST'])
def user_details():
    email = request.form.get('email')
    
    # Get account details
    account_query = f"""
    SELECT 
        EMAIL, 
        ACCOUNT_ID, 
        ACCOUNT_NAME, 
        ACCOUNT_NUMBER, 
        ACCOUNT_TYPE, 
        ACCOUNT_SUB_TYPE, 
        ACCOUNT_BALANCE 
    FROM PAY_ZONE_SCHEMA.ACCOUNT_TABLE 
    WHERE EMAIL = '{email}'
    """
    account_details = execute_snowflake(account_query)
    account_number = account_details[0][3]
    # Get transactions
    transactions_query = f"""
    SELECT 
        TRANSACTION_ID, 
        DEBITOR_ACCOUNT, 
        CREDITOR_ACCOUNT, 
        AMOUNT, 
        CONTRACT_ID, 
        TRANSACTION_TIME 
    FROM PAY_ZONE_SCHEMA.TRANSACTIONS 
    WHERE DEBITOR_ACCOUNT = '{account_number}' OR CREDITOR_ACCOUNT = '{account_number}'
    ORDER BY TRANSACTION_TIME DESC
    """
    transactions = execute_snowflake(transactions_query)

    response_data = {
        'account_details': account_details,
        'transactions': transactions
    }
    
    return json.dumps(response_data, default=str)
    
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=4061, debug=True)
