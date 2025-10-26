export default function Footer() {
  return (
    <footer className="bg-blue-700 text-white py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h4 className="font-bold text-lg mb-4">Postal Address</h4>
            <p className="text-blue-100">
              Faculty of Engineering,<br />
              University of Ruhuna,<br />
              Hapugala, Galle,<br />
              Sri Lanka.<br />
              80000
            </p>
          </div>
          <div>
            <h4 className="font-bold text-lg mb-4">Phone Number</h4>
            <p className="text-blue-100">
              +(94) 912245765,<br />
              +(94) 912245766,<br />
              +(94) 912245767
            </p>
          </div>
          <div>
            <h4 className="font-bold text-lg mb-4">Other</h4>
            <p className="text-blue-100">
              Fax : +94 912245762<br />
              Email : ar@eng.ruh.ac.lk
            </p>
          </div>
        </div>

        <div className="border-t border-blue-600 mt-8 pt-8 text-center text-blue-100">
          <p>&copy; 2025 LO-PO System. All rights reserved.</p>
        </div>
      </div>
    </footer>
  )
}