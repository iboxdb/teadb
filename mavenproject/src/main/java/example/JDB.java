package example;
 
import java.math.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import iBoxDB.LocalServer.*;
import iBoxDB.LocalServer.IO.*;
import iBoxDB.LocalServer.Replication.*;

//PC: DB.root("/tmp/");
//Android: initAndroid("com.example.fapp");
//--
//System.out.println(example.JDB.run());

public class JDB {
	private static boolean isAndroid = false;

/*
	public static void initAndroid(String packageName) {
		isAndroid = true;
		DB.root(android.os.Environment.getDataDirectory().getAbsolutePath()
				+ "/data/" + packageName + "/");
	}
*/
	public static String run() {
		return run(false);
	}

	public static String run(boolean speedTest) {

		try {
			// DB.root("/tmp/");
			strout = new StringBuilder();

			println("");
			GettingStarted();

			println("");
			IsolatedSpace();

			println("");
			BeyondSQL();

			println("");
			MasterSlave();

			println("");
			MasterMaster();

			if (speedTest) {
				System.gc();
				println("");
				SpeedTest();

				System.gc();
				println("");
				ReplicationSpeed(10);
			}

			helper.deleteDB();
			return println("").toString();
		} catch (Throwable ex) {
			return ex.getMessage();
		}
	}

	public static void GettingStarted() {
		println("Getting Started");
		helper.deleteDB();

		DB db = new DB(1);
		db.getConfig().ensureTable("Table", Member.class, "ID");
		AutoBox auto = db.open();

		long key = 1;
		auto.insert("Table", new Member(key, "Andy"));
		Member o1 = auto.get(Member.class, "Table", key);
		println(o1.getName());

		o1.setName("Kelly");
		auto.update("Table", o1);
		o1 = null;

		Member o2 = auto.get(Member.class, "Table", key);
		println(o2.getName());

		db.close();
	}

	public static void IsolatedSpace() {
		println("Isolated Space");
		helper.deleteDB();

		DB db = new DB(1);

		db.getConfig().ensureTable(Member.class, "Member", "ID");
		// stringColumn(length), default length is 32
		db.getConfig().ensureIndex(Member.class, "Member", "Name(20)");
		// particular index for 'MemberVIP.VIP' column
		db.getConfig().ensureIndex(MemberVIP.class, "Member", "VIP");

		// Composite Key Supported
		db.getConfig().ensureTable(Product.class, "Product", "Type", "UID");
		AutoBox auto = db.open();

		// Creating Isolated Space, the Box.
		// java7+ try( Box box = auto.cube() ){ }
		long andyId, kellyId;
		Box box = auto.cube();
		try {
			andyId = box.newId();
			kellyId = box.newId();
			// insert members, two different classes with different index
			// setting.
			Member m = new Member();
			m.ID = andyId;
			m.setName("Andy");
			m.setRegTime((new GregorianCalendar(2013, 1, 2)).getTime());
			m.setTags(new Object[] { "Nice", "Player" });
			box.d("Member").insert(m);
			m = null;

			MemberVIP mvip = new MemberVIP();
			mvip.ID = kellyId;
			mvip.setName("Kelly");
			mvip.setRegTime((new GregorianCalendar(2013, 1, 3)).getTime());
			mvip.setTags(new Object[] { "Player" });
			mvip.VIP = 3;
			box.d("Member").insert(mvip);
			mvip = null;

			Product game = new Product();
			game.Type(8);
			game.UID(UUID.fromString("22222222-0000-0000-0000-000000000000"));
			game.Name("MoonFlight");
			// Dynamic Column
			game.put("GameType", "ACT");
			box.d("Product").insert(game);

			CommitResult cr = box.commit();
			// cr.equals(CommitResult.OK)
			cr.Assert();
		} finally {
			box.close();
		}

		box = auto.cube();
		try {
			// Query Object, SQL Style
			// > < >= <= == !=
			// & | ()
			// []
			// case sensitive:
			// Name -> Name , name -> name
			// getName()->Name , getname()->name
			// Name()->Name, name()->name
			for (MemberVIP m : box.select(MemberVIP.class,
					"from Member where VIP>?", 1)) {
				println("Member: " + m.getName() + " RegTime: "
						+ m.getRegTime());
			}

			// Key-Value Style, Composite-Key Supported
			Product cs = box.d("Product", 8,
					UUID.fromString("22222222-0000-0000-0000-000000000000"))
					.select(Product.class);
			println("Product: " + cs.Name() + "  Type: " + cs.get("GameType"));

			box.commit().Assert();
		} finally {
			box.close();
		}

		box = auto.cube();
		try {
			MemberVIP mvip = box.d("Member", kellyId).select(MemberVIP.class);
			// Update Amount and Name
			mvip.setName("Kelly J");
			mvip.setAmount(BigDecimal.valueOf(100.0));
			box.d("Member", mvip.ID).update(mvip);
			box.commit().Assert();
		} finally {
			box.close();
		}
		box = auto.cube();
		try {
			for (MemberVIP m : box.select(MemberVIP.class,
					"from Member where Name==?", "Kelly J")) {
				println("Updated: " + m.getName() + "  Amount: "
						+ m.getAmount());
			}
		} finally {
			box.close();
		}
		db.close();
	}

	public static void BeyondSQL() {
		helper.deleteDB();

		DB db = new DB(1);
		db.getConfig().ensureTable(MemberInc.class, "MemberInc", "ID");
		db.getConfig().ensureUpdateIncrementIndex(MemberInc.class, "MemberInc",
				"Version");
		AutoBox auto = db.open();

		println("Update Increment");
		print("Number increasing: ");

		MemberInc m = new MemberInc();
		m.ID = 1;
		m.setName("Andy");

		auto.insert("MemberInc", m);
		MemberInc mg = auto.get(MemberInc.class, "MemberInc", 1L);
		print(mg.Version + ".  ");

		auto.update("MemberInc", mg);
		mg = auto.get(MemberInc.class, "MemberInc", 1L);
		print(mg.Version + ".  ");

		auto.update("MemberInc", mg);
		mg = auto.get(MemberInc.class, "MemberInc", 1L);
		println(mg.Version + ".  ");

		println("Selecting Tracer");
		Box boxTracer = auto.cube();
		try {
			boolean keepTrace = true;
			Member tra = boxTracer.d("MemberInc", 1L).select(Member.class,
					keepTrace);
			String currentName = tra.getName();

			{
				// another box changes the name
				MemberInc mm = new MemberInc();
				mm.ID = 1;
				mm.setName("Kelly");
				auto.update("MemberInc", mm.ID, mm);
			}

			// Change detected
			if (!boxTracer.commit().equals(CommitResult.OK)) {
				print("Detected '" + currentName + "' has changed, ");
			}
		} finally {
			boxTracer.close();
		}
		Member nm = auto.get(Member.class, "MemberInc", 1L);
		println("new name is '" + nm.getName() + "'");

		db.close();
	}

	public static void MasterSlave() {
		helper.deleteDB();
		long MasterA_DBAddress = 10;
		// negative number
		long SlaveA_DBAddress = -10;

		DB db = new DB(MasterA_DBAddress);
		db.getConfig().ensureTable(Member.class, "Member", "ID");
		db.setBoxRecycler(new MemoryBoxRecycler());
		AutoBox auto = db.open();

		DB db_slave = new DB(SlaveA_DBAddress);
		AutoBox auto_slave = db_slave.open();

		println("MasterSlave Replication");
		Box box = auto.cube();
		try {
			for (int i = 0; i < 3; i++) {
				Member m = new Member();
				m.ID = box.newId();
				m.setName("S " + i);
				box.d("Member").insert(m);
			}
			box.commit().Assert();
		} finally {
			box.close();
		}

		// Database Serialization
		MemoryBoxRecycler recycler = (MemoryBoxRecycler) auto.getDatabase()
				.getBoxRecycler();
		synchronized (recycler.packages) {
			for (Package p : recycler.packages) {
				if (p.Socket.SourceAddress == MasterA_DBAddress) {
					(new BoxData(p.OutBox)).slaveReplicate(
							auto_slave.getDatabase()).Assert();
				}
			}
			recycler.packages.clear();
		}

		println("Master Address: " + auto.getDatabase().localAddress()
				+ ", Data:");
		for (Member o : auto.select(Member.class, "from Member")) {
			print(o.getName() + ". ");
		}
		println("");

		println("Slave Address: " + auto_slave.getDatabase().localAddress()
				+ ", Data:");
		for (Member o : auto_slave.select(Member.class, "from Member")) {
			print(o.getName() + ". ");
		}
		println("");

		db.close();
		db_slave.close();
	}

	public static void MasterMaster() {
		helper.deleteDB();
		long MasterA_DBAddress = 10;
		long MasterB_DBAddress = 20;

		DB db_masterA = new DB(MasterA_DBAddress);
		db_masterA.getConfig().ensureTable(Member.class, "Member", "ID");
		db_masterA.setBoxRecycler(new MemoryBoxRecycler());
		// send to MasterB_DBAddress
		AutoBox auto_masterA = db_masterA.open(MasterB_DBAddress);

		DB db_masterB = new DB(MasterB_DBAddress);
		db_masterB.getConfig().ensureTable(Member.class, "Member", "ID");
		db_masterB.setBoxRecycler(new MemoryBoxRecycler());
		// send to MasterA_DBAddress
		AutoBox auto_masterB = db_masterB.open(MasterA_DBAddress);

		println("MasterMaster Replication");
		byte IncTableID = 1;

		Box box = auto_masterA.cube();
		try {
			for (int i = 0; i < 3; i++) {
				Member m = new Member();
				m.ID = box.newId(IncTableID, 1) * 1000 + MasterA_DBAddress;
				m.setName("A" + i);
				box.d("Member").insert(m);
			}
			box.commit().Assert();
		} finally {
			box.close();
		}

		box = auto_masterB.cube();
		try {
			for (int i = 0; i < 3; i++) {
				Member m = new Member();
				m.ID = box.newId(IncTableID, 1) * 1000 + MasterB_DBAddress;
				m.setName("B" + i);
				box.d("Member").insert(m);
			}
			box.commit().Assert();
		} finally {
			box.close();
		}

		// Do Replication
		ArrayList<Package> buffer;
		MemoryBoxRecycler recycler = (MemoryBoxRecycler) auto_masterA
				.getDatabase().getBoxRecycler();
		synchronized (recycler.packages) {
			buffer = new ArrayList<Package>(recycler.packages);
			recycler.packages.clear();
		}
		recycler = (MemoryBoxRecycler) auto_masterB.getDatabase()
				.getBoxRecycler();
		synchronized (recycler.packages) {
			buffer.addAll(recycler.packages);
			recycler.packages.clear();
		}
		for (Package p : buffer) {
			if (p.Socket.DestAddress == MasterA_DBAddress) {
				(new BoxData(p.OutBox)).masterReplicate(auto_masterA
						.getDatabase());
			}
			if (p.Socket.DestAddress == MasterB_DBAddress) {
				(new BoxData(p.OutBox)).masterReplicate(auto_masterB
						.getDatabase());
			}
		}

		println("MasterA Address: " + auto_masterA.getDatabase().localAddress());
		for (Map<String, Object> o : auto_masterA.select("from Member")) {
			print(o.get("Name") + ". ");
		}
		println("");

		println("MasterB Address: " + auto_masterB.getDatabase().localAddress());
		for (Map<String, Object> o : auto_masterB.select("from Member")) {
			print(o.get("Name") + ". ");
		}

		println("");
		db_masterA.close();
		db_masterB.close();
		/*
		 * another replication config Key = [ID,Address] m.ID =
		 * box.NewId(IncTableID, 1) ; m.Address = box.LocalAddress;
		 * box.Bind("Member").Insert(m);
		 */
	}

	public static void SpeedTest() throws InterruptedException {
		helper.deleteDB();
		DB db = new DB(1);
		db.getConfig().ensureTable(Member.class, "TSpeed", "ID");
		DatabaseConfig dbConfig = db.getConfig().DBConfig;
		// Cache
		// dbConfig.CacheLength = dbConfig.mb(512);
		// File
		// dbConfig.FileIncSize = (int) dbConfig.mb(4);
		// Thread
		// dbConfig.ReadStreamCount = 8;
		final AutoBox auto = db.open();

		println("Speed");

		final int threadCount = isAndroid ? 20 : 20000;
		final int objectCount = 10;
		final int poolCount = isAndroid ? 2 : 8;
		println("Begin Insert " + helper.format(threadCount * objectCount)
				+ " objects");
		long begin = System.currentTimeMillis();
		ExecutorService pool = Executors.newFixedThreadPool(poolCount);
		for (int i = 0; i < threadCount; i++) {
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Box box = auto.cube();
					try {
						for (int o = 0; o < objectCount; o++) {
							Member m = new Member();
							m.ID = box.newId(0, 1);
							m.setName(o + "_" + m.ID);
							m.setAge(1);

							box.d("TSpeed").insert(m);
						}
						box.commit().Assert();
					} finally {
						box.close();
					}
				}
			});
		}
		pool.shutdown();
		pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

		double sec = (System.currentTimeMillis() - begin) / 1000.0;
		double avg = (threadCount * objectCount) / sec;
		println("Elapsed " + helper.getDou(sec) + "s, AVG Insert "
				+ helper.format(avg) + " o/sec");
		System.gc();
		System.runFinalization();

		begin = System.currentTimeMillis();
		pool = Executors.newFixedThreadPool(poolCount);
		for (int fi = 0; fi < threadCount; fi++) {
			final int i = fi;
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Box box = auto.cube();
					try {
						for (int o = 0; o < objectCount; o++) {
							long ID = i * objectCount + o + 1;
							Member mem = box.d("TSpeed", ID).select(
									Member.class);
							if (mem.ID != ID) {
								throw new RuntimeException();
							}
						}
					} finally {
						box.close();
					}
				}
			});
		}
		pool.shutdown();
		pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		sec = (System.currentTimeMillis() - begin) / 1000.0;
		avg = (threadCount * objectCount) / sec;
		println("Elapsed " + helper.getDou(sec) + "s, AVG Lookup "
				+ helper.format(avg) + " o/sec");
		System.gc();
		System.runFinalization();

		final AtomicInteger count = new AtomicInteger(0);
		begin = System.currentTimeMillis();
		pool = Executors.newFixedThreadPool(poolCount);
		for (int i = 0; i < threadCount; i++) {
			final int finalI = i;
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Box box = auto.cube();
					try {
						Iterable<Member> tspeed = box.select(Member.class,
								"from TSpeed where ID>=? & ID<=?",
								(long) (finalI * objectCount + 1),
								(long) (finalI * objectCount + objectCount));
						for (Member m : tspeed) {
							// age == 1
							count.addAndGet(m.getAge());
						}
					} finally {
						box.close();
					}

				}
			});
		}
		pool.shutdown();
		pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		sec = (System.currentTimeMillis() - begin) / 1000.0;
		avg = (threadCount * objectCount) / sec;

		if (count.get() != (threadCount * objectCount)) {
			throw new RuntimeException("e " + count.get());
		}

		println("Elapsed " + helper.getDou(sec) + "s, AVG Query "
				+ helper.format(avg) + " o/sec");

		db.close();
	}

	public static void ReplicationSpeed(int time) throws InterruptedException {
		helper.deleteDB();
		int MasterA_DBAddress = 10;
		int MasterB_DBAddress = 20;

		int SlaveA_DBAddress = -10;

		DB db_masterA = new DB(MasterA_DBAddress);
		db_masterA.getConfig().ensureTable(Member.class, "TSpeed", "ID");
		db_masterA.setBoxRecycler(new MemoryBoxRecycler());
		final AutoBox auto_masterA = db_masterA.open(MasterB_DBAddress);

		DB db_slave = new DB(SlaveA_DBAddress);
		final AutoBox auto_slave = db_slave.open();

		DB db_masterB = new DB(MasterB_DBAddress);
		db_masterB.getConfig().ensureTable(Member.class, "TSpeed", "ID");
		final AutoBox auto_masterB = db_masterB.open();

		BoxData[] data = ((MemoryBoxRecycler) auto_masterA.getDatabase()
				.getBoxRecycler()).asBoxData();
		BoxData.slaveReplicate(auto_slave.getDatabase(), data).Assert();
		BoxData.masterReplicate(auto_masterB.getDatabase(), data).Assert();

		int threadCount = 200;
		if (isAndroid) {
			threadCount = 2;
			time = time > 2 ? 2 : time;
		}
		final int objectCount = 10;

		double slaveSec = 0;
		double masterSec = 0;

		final int poolCount = isAndroid ? 2 : 8;

		for (int t = 0; t < time; t++) {
			ExecutorService pool = Executors.newFixedThreadPool(poolCount);
			for (int i = 0; i < threadCount; i++) {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						Box box = auto_masterA.cube();
						try {
							for (int o = 0; o < objectCount; o++) {
								Member m = new Member();
								m.ID = box.newId(0, 1);
								m.setName(m.ID + "_" + o);
								m.setAge(1);
								box.d("TSpeed").insert(m);
							}
							box.commit().Assert();
						} finally {
							box.close();
						}
					}
				});
			}
			pool.shutdown();
			pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			data = ((MemoryBoxRecycler) auto_masterA.getDatabase()
					.getBoxRecycler()).asBoxData();

			long begin = System.currentTimeMillis();
			BoxData.slaveReplicate(auto_slave.getDatabase(), data).Assert();
			slaveSec += ((System.currentTimeMillis() - begin) / 1000.0);

			begin = System.currentTimeMillis();
			BoxData.masterReplicate(auto_masterB.getDatabase(), data).Assert();
			masterSec += ((System.currentTimeMillis() - begin) / 1000.0);

		}
		println("Replicate " + (threadCount * time) + " transactions, totals "
				+ helper.format(threadCount * objectCount * time) + " objects");
		double avg = (threadCount * objectCount * time) / slaveSec;
		println("SlaveSpeed " + helper.getDou(slaveSec) + "s, AVG "
				+ helper.format(avg) + " o/sec");

		avg = (threadCount * objectCount * time) / masterSec;
		println("MasterSpeed " + helper.getDou(masterSec) + "s, AVG "
				+ helper.format(avg) + " o/sec");

		final AtomicInteger count = new AtomicInteger(0);

		ExecutorService pool = Executors.newFixedThreadPool(poolCount);
		long begin = System.currentTimeMillis();
		final int fthreadCount = threadCount;
		for (int ft = 0; ft < time; ft++) {
			final int t = ft;
			for (int fi = 0; fi < threadCount; fi++) {
				final int i = fi;
				pool.execute(new Runnable() {
					@Override
					public void run() {
						for (int dbc = 0; dbc < 2; dbc++) {
							Box box = dbc == 0 ? auto_slave.cube()
									: auto_masterB.cube();
							try {
								for (int o = 0; o < objectCount; o++) {
									long ID = i * objectCount + o + 1;
									ID += (t * fthreadCount * objectCount);
									Member mem = box.d("TSpeed", ID).select(
											Member.class);
									if (mem.ID != ID) {
										throw new RuntimeException();
									}
									count.addAndGet(mem.getAge());
								}
							} finally {
								box.close();
							}
						}
					}
				});
			}
		}
		pool.shutdown();
		pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		double sec = (System.currentTimeMillis() - begin) / 1000.0;
		if (count.get() != (threadCount * objectCount * time * 2)) {
			throw new RuntimeException();
		}
		avg = count.get() / sec;
		println("Lookup after replication " + helper.getDou(sec) + "s, AVG "
				+ helper.format(avg) + " o/sec");

		if (count.get() != auto_slave.selectCount("from TSpeed")
				+ auto_masterB.selectCount("from TSpeed")) {
			throw new RuntimeException();
		}

		auto_masterA.getDatabase().close();
		auto_slave.getDatabase().close();
		auto_masterB.getDatabase().close();
	}

	public static abstract class IDClass {
		public long ID;
	}

	public static class Member extends IDClass {

		public Member() {
		};

		public Member(long id, String name) {
			ID = id;
			_name = name;
		};

		private String _name;
		private Date _regTime;
		private Object[] _tags;
		private BigDecimal _amount;
		private int _age;

		public int getAge() {
			return _age;
		}

		public void setAge(int value) {
			_age = value;
		}

		public Date getRegTime() {
			return _regTime;
		}

		public void setRegTime(Date value) {
			_regTime = value;
		}

		public String getName() {
			return _name;
		}

		public void setName(String value) {
			_name = value;
		}

		public Object[] getTags() {
			return _tags;
		}

		public void setTags(Object[] value) {
			_tags = value;
		}

		public BigDecimal getAmount() {
			return _amount;
		}

		public void setAmount(BigDecimal value) {
			_amount = value;
		}

	}

	public static class MemberVIP extends Member {
		public int VIP;
	}

	public static class MemberInc extends Member {
		// increment type is long
		public long Version;
	}

	public static class Product extends HashMap<String, Object> {

		public int Type() {
			return (Integer) this.get("Type");
		}

		public void Type(int value) {
			this.put("Type", value);
		}

		public UUID UID() {

			return (UUID) this.get("UID");
		}

		public void UID(UUID value) {
			this.put("UID", value);

		}

		public String Name() {
			return (String) this.get("Name");
		}

		public void Name(String value) {
			this.put("Name", value);
		}
	}

	public static class Package {

		public Package(Socket socket2, byte[] outBox2) {
			Socket = socket2;
			OutBox = outBox2;
		}

		public Socket Socket;
		public byte[] OutBox;
	}

	// recycle boxes
	public static class MemoryBoxRecycler extends IBoxRecycler /* extends IBoxRecycler3 */{ 
		public ArrayList<Package> packages = new ArrayList<Package>();

		public MemoryBoxRecycler() {
		}

		public MemoryBoxRecycler(String name, DatabaseConfig config) {
			this();
		}

		public void onReceived(Socket socket, BoxData outBox, boolean normal) {
			if (socket.DestAddress == Long.MAX_VALUE) {
				// default replicate address
				return;
			}
			synchronized (packages) {
				packages.add(new Package(socket, outBox.toBytes()));
			}
		}

		public BoxData[] asBoxData() {
			synchronized (packages) {
				ArrayList<BoxData> list = new ArrayList<BoxData>();
				for (Package p : packages) {
					list.add(new BoxData(p.OutBox));
				}
				packages.clear();
				return list.toArray(new BoxData[list.size()]);
			}
		}

		@Override
		public void close() {
			packages = null;
		}
	}

	// ALL in One Config, var server = new ApplicationServer();
	public static class ApplicationServer extends LocalDatabaseServer {
		public static class PlatformConfig extends BoxFileStreamConfig {
			public PlatformConfig() {
				CacheLength = mb(512);
				FileIncSize = (int) mb(4);
				ReadStreamCount = 8;
			}
		}

		public static class MyConfig extends PlatformConfig {
			public MyConfig(long addr) {

				EnsureTable(Member.class, "Member", "ID");
				EnsureIndex(Member.class, "Member", "Name(20)");
				EnsureIndex(MemberVIP.class, "Member", "VIP");

				EnsureTable(Product.class, "Product", "Type", "UID");

				EnsureTable(Member.class, "TSpeed", "ID");

				EnsureTable(MemberInc.class, "MemberInc", "ID");
				this.EnsureUpdateIncrementIndex(MemberInc.class, "MemberInc",
						"Version");

				// KeyOnly Table, StartsWith '/', only read/write ID and Name
				EnsureTable(Member.class, "/M", "ID", "Name");
			}
		}

		public static final int MasterA_Address = 10;
		public static final int MasterB_Address = 20;

		// Slave is negative number
		public static final int SlaveA_Address = -10;

		protected DatabaseConfig BuildDatabaseConfig(long address) {
			if (address == SlaveA_Address) {
				return new PlatformConfig();
			}
			if (address == MasterA_Address || address == MasterB_Address) {
				return new MyConfig(address);
			}
			throw new RuntimeException();
		}

		protected IBoxRecycler BuildBoxRecycler(long address,
				DatabaseConfig config) {
			if (address == MasterA_Address) {
				return new MemoryBoxRecycler(GetNameByAddr(address), config);
			}
			if (address == MasterB_Address) {
				return new MemoryBoxRecycler(GetNameByAddr(address), config);
			}
			return super.BuildBoxRecycler(address, config);
		}
	}

	public static class helper {
		public static void deleteDB() {
			if (!BoxSystem.DBDebug.DeleteDBFiles(1, 10, 20, -10)) {
				System.out.println("delete=false,system locks");
			}
		}

		public static String getDou(double d) {
			long l = (long) (d * 1000);
			return Double.toString(l / 1000.0);
		}

		public static String format(double d) {
			return NumberFormat.getInstance().format((int) d);
		}
	}

	private static StringBuilder strout;

	private static StringBuilder println(String msg) {
		strout.append(msg + "\r\n");
		return strout;
	}

	private static StringBuilder print(String msg) {
		strout.append(msg);
		return strout;
	}
}
